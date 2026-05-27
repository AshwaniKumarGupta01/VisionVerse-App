package com.example.visionverse.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visionverse.ui.viewmodels.ExamViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class ExamState { INSTRUCTIONS, QUESTION, CONFIRMATION, EARLY_SUBMIT_CONFIRM, FINISHED }

@Composable
fun ExamScreen(
    subjectId: String,
    onExamComplete: (Int, Int) -> Unit,
    viewModel: ExamViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF0A0F1A), Color(0xFF009688))
    )

    val questions by viewModel.questions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(subjectId) {
        viewModel.fetchQuestionsForSubject(subjectId)
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(gradientBrush), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF2ADECD))
                Text("Loading Exam...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
            }
        }
        return
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(gradientBrush), contentAlignment = Alignment.Center) {
            Text("No questions found for this subject.", color = Color.White)
        }
        return
    }

    // --- EXAM DATA ---
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    var appState by remember { mutableStateOf(ExamState.INSTRUCTIONS) }

    val totalQuestions = questions.size
    val currentQuestion = questions[currentQuestionIndex].text
    val options = questions[currentQuestionIndex].options

    // 🔥 GUARANTEED NAVIGATION 🔥
    LaunchedEffect(appState) {
        if (appState == ExamState.FINISHED) {
            delay(2500)
            try { (context as? Activity)?.stopLockTask() } catch (e: Exception) {}
            onExamComplete(score, totalQuestions)
        }
    }

    val instructionsText = "Welcome to the Vision Verse Exam. I will read each question out loud. Answer by saying Option A, B, C, or D, OR by holding up 1, 2, 3, or 4 fingers to the camera. Hold up a closed fist to skip, Thumb Up to confirm, Thumb Down to cancel, and an open hand with 5 fingers to submit early. Say 'Start' to begin."

    // --- AI & SYSTEM VARIABLES ---
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var spokenFeedback by remember { mutableStateOf("Initializing...") }
    var outOfAppWarnings by remember { mutableIntStateOf(0) }
    var isBackgrounded by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // --- MEDIAPIPE GESTURE VARIABLES ---
    var detectedGesture by remember { mutableStateOf("") }
    var handLandmarker by remember { mutableStateOf<HandLandmarker?>(null) }
    val backgroundExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 100000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 100000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100000L)
        }
    }

    val speakText = { text: String, utteranceId: String ->
        mainHandler.post {
            isSpeaking = true
            isListening = false
            speechRecognizer.cancel()
            spokenFeedback = "Speaking..."
            val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    // --- SETUP MEDIAPIPE HAND LANDMARKER ---
    LaunchedEffect(Unit) {
        try {
            val baseOptions = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    if (result.landmarks().isNotEmpty()) {
                        val lm = result.landmarks()[0]

                        // Check if fingers are extended by comparing Tip to PIP joint y-coordinates
                        val isIndexUp = lm[8].y() < lm[6].y()
                        val isMiddleUp = lm[12].y() < lm[10].y()
                        val isRingUp = lm[16].y() < lm[14].y()
                        val isPinkyUp = lm[20].y() < lm[18].y()

                        val fingersCount = listOf(isIndexUp, isMiddleUp, isRingUp, isPinkyUp).count { it }

                        // Thumb logic: Is the thumb tip higher than the thumb base, and are other fingers folded?
                        val isThumbUp = lm[4].y() < lm[3].y() && lm[4].y() < lm[5].y() && fingersCount == 0
                        val isThumbDown = lm[4].y() > lm[3].y() && lm[4].y() > lm[5].y() && fingersCount == 0

                        val action = when {
                            fingersCount == 4 && lm[4].y() < lm[5].y() -> "SUBMIT" // 5 Fingers
                            isThumbUp -> "THUMB_UP"
                            isThumbDown -> "THUMB_DOWN"
                            fingersCount == 0 -> "SKIP" // Fist
                            fingersCount in 1..4 -> fingersCount.toString()
                            else -> ""
                        }

                        if (action.isNotEmpty() && detectedGesture != action) {
                            detectedGesture = action
                        }
                    } else {
                        if (detectedGesture != "") detectedGesture = ""
                    }
                }
                .setErrorListener { error -> Log.e("MediaPipe", "Error: ${error.message}") }
            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            Log.e("MediaPipe", "Model not found! Make sure hand_landmarker.task is in assets folder.")
        }
    }

    // --- GESTURE TO ANSWER LOGIC WITH DEBOUNCE ---
    LaunchedEffect(detectedGesture) {
        if (detectedGesture.isEmpty() || isSpeaking) return@LaunchedEffect

        // Wait for 1.2 seconds of holding the gesture to confirm it (Prevents accidental triggers)
        delay(1200)

        when {
            detectedGesture == "SUBMIT" -> {
                if (appState != ExamState.INSTRUCTIONS && appState != ExamState.FINISHED) {
                    appState = ExamState.EARLY_SUBMIT_CONFIRM
                    speakText("Are you sure you want to submit early? Show Thumb Up for Yes, or Thumb Down for No.", "TTS_EARLY_SUBMIT")
                }
            }
            appState == ExamState.EARLY_SUBMIT_CONFIRM -> {
                if (detectedGesture == "THUMB_UP") {
                    appState = ExamState.FINISHED
                    speakText("Exam complete. Submitting your test.", "TTS_END")
                } else if (detectedGesture == "THUMB_DOWN") {
                    appState = ExamState.QUESTION
                    val textToRead = "Continuing exam. Question ${currentQuestionIndex + 1}. $currentQuestion... ${options[0]}... ${options[1]}... ${options[2]}... ${options[3]}."
                    speakText(textToRead, "TTS_QUESTION")
                }
            }
            detectedGesture == "SKIP" -> {
                if (appState != ExamState.INSTRUCTIONS && appState != ExamState.EARLY_SUBMIT_CONFIRM) {
                    if (currentQuestionIndex < totalQuestions - 1) {
                        currentQuestionIndex++
                        selectedOption = null
                        appState = ExamState.QUESTION
                        speakText("Skipped. Question ${currentQuestionIndex + 1}. ${questions[currentQuestionIndex].text}... ${questions[currentQuestionIndex].options[0]}... ${questions[currentQuestionIndex].options[1]}... ${questions[currentQuestionIndex].options[2]}... ${questions[currentQuestionIndex].options[3]}.", "TTS_QUESTION")
                    } else {
                        appState = ExamState.FINISHED
                        speakText("Exam complete. Submitting your test.", "TTS_END")
                    }
                }
            }
            detectedGesture == "THUMB_UP" -> {
                if (appState == ExamState.CONFIRMATION) {
                    if (selectedOption == questions[currentQuestionIndex].correctAnswerIndex) score++
                    if (currentQuestionIndex < totalQuestions - 1) {
                        currentQuestionIndex++
                        selectedOption = null
                        appState = ExamState.QUESTION
                        speakText("Confirmed. Question ${currentQuestionIndex + 1}. ${questions[currentQuestionIndex].text}... ${questions[currentQuestionIndex].options[0]}... ${questions[currentQuestionIndex].options[1]}... ${questions[currentQuestionIndex].options[2]}... ${questions[currentQuestionIndex].options[3]}.", "TTS_QUESTION")
                    } else {
                        appState = ExamState.FINISHED
                        speakText("Exam complete. Submitting your test.", "TTS_END")
                    }
                }
            }
            detectedGesture in listOf("1", "2", "3", "4") -> {
                if (appState == ExamState.QUESTION || appState == ExamState.CONFIRMATION) {
                    val mappedOption = detectedGesture.toInt() - 1
                    selectedOption = mappedOption
                    appState = ExamState.CONFIRMATION
                    speakText("You selected ${options[selectedOption!!]}. Show Thumb Up to confirm, or a different number to change.", "TTS_CONFIRM")
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (appState != ExamState.INSTRUCTIONS && appState != ExamState.FINISHED) {
                    isBackgrounded = true
                    speechRecognizer.cancel()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (isBackgrounded && appState != ExamState.FINISHED) {
                    isBackgrounded = false
                    outOfAppWarnings++
                    if (outOfAppWarnings < 3) {
                        speakText("Warning! You have ${3 - outOfAppWarnings} chances left before auto-submit.", "TTS_WARNING")
                    } else {
                        appState = ExamState.FINISHED
                        speakText("You have left the application 3 times. Submitting.", "TTS_END")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val startListening = {
        mainHandler.post {
            if (appState != ExamState.FINISHED && !isSpeaking) {
                try {
                    speechRecognizer.cancel()
                    isListening = true
                    spokenFeedback = "Listening continuously..."
                    speechRecognizer.startListening(speechIntent)
                } catch (e: Exception) {}
            }
        }
    }

    // --- PERMISSIONS ---
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasPermissions = perms[Manifest.permission.RECORD_AUDIO] == true && perms[Manifest.permission.CAMERA] == true
        if (hasPermissions) speakText(instructionsText, "TTS_INSTRUCTIONS")
    }

    LaunchedEffect(Unit) {
        try { (context as? Activity)?.startLockTask() } catch (e: Exception) {}
        if (!hasPermissions) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        }
    }

    DisposableEffect(context, currentQuestionIndex) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                if (!isSpeaking && appState != ExamState.FINISHED) mainHandler.postDelayed({ startListening() }, 500)
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val heardText = matches[0].lowercase(Locale.getDefault())
                    spokenFeedback = "Heard: \"$heardText\""
                    var validCommandGiven = true

                    when {
                        heardText.contains("submit") -> {
                            if (appState != ExamState.INSTRUCTIONS && appState != ExamState.FINISHED) {
                                appState = ExamState.EARLY_SUBMIT_CONFIRM
                                speakText("Submit early? Say yes or no.", "TTS_EARLY_SUBMIT")
                            } else validCommandGiven = false
                        }
                        appState == ExamState.EARLY_SUBMIT_CONFIRM -> {
                            when {
                                heardText.contains("yes") -> { appState = ExamState.FINISHED; speakText("Submitting.", "TTS_END") }
                                heardText.contains("no") -> {
                                    appState = ExamState.QUESTION
                                    speakText("Continuing. ${currentQuestionIndex + 1}. $currentQuestion", "TTS_QUESTION")
                                }
                                else -> validCommandGiven = false
                            }
                        }
                        heardText.contains("start") || heardText.contains("skip") -> {
                            if (appState == ExamState.INSTRUCTIONS && heardText.contains("start")) {
                                appState = ExamState.QUESTION
                                speakText("Question 1. $currentQuestion... ${options[0]}... ${options[1]}... ${options[2]}... ${options[3]}.", "TTS_QUESTION")
                            } else if (appState != ExamState.INSTRUCTIONS && heardText.contains("skip")) {
                                if (currentQuestionIndex < totalQuestions - 1) {
                                    currentQuestionIndex++
                                    selectedOption = null
                                    appState = ExamState.QUESTION
                                    speakText("Question ${currentQuestionIndex + 1}. ${questions[currentQuestionIndex].text}", "TTS_QUESTION")
                                } else {
                                    appState = ExamState.FINISHED
                                    speakText("Exam complete.", "TTS_END")
                                }
                            } else validCommandGiven = false
                        }
                        heardText.contains("repeat") -> {
                            val textToRead = if (appState == ExamState.INSTRUCTIONS) instructionsText
                            else "Question ${currentQuestionIndex + 1}. $currentQuestion"
                            speakText(textToRead, "TTS_QUESTION")
                        }
                        heardText.contains("next") -> {
                            if (appState == ExamState.CONFIRMATION) {
                                if (selectedOption == questions[currentQuestionIndex].correctAnswerIndex) score++
                                if (currentQuestionIndex < totalQuestions - 1) {
                                    currentQuestionIndex++
                                    selectedOption = null
                                    appState = ExamState.QUESTION
                                    speakText("Question ${currentQuestionIndex + 1}. ${questions[currentQuestionIndex].text}", "TTS_QUESTION")
                                } else {
                                    appState = ExamState.FINISHED
                                    speakText("Exam complete.", "TTS_END")
                                }
                            } else speakText("Select an option first.", "TTS_ERROR")
                        }
                        appState == ExamState.QUESTION || appState == ExamState.CONFIRMATION -> {
                            when {
                                heardText.contains("a") || heardText.contains("one") -> selectedOption = 0
                                heardText.contains("b") || heardText.contains("two") -> selectedOption = 1
                                heardText.contains("c") || heardText.contains("three") -> selectedOption = 2
                                heardText.contains("d") || heardText.contains("four") -> selectedOption = 3
                                else -> validCommandGiven = false
                            }
                            if (validCommandGiven) {
                                appState = ExamState.CONFIRMATION
                                speakText("Selected ${options[selectedOption!!]}. Say next.", "TTS_CONFIRM")
                            }
                        }
                        else -> validCommandGiven = false
                    }
                    if (!validCommandGiven && !isSpeaking) mainHandler.postDelayed({ startListening() }, 200)
                } else {
                    if (!isSpeaking) mainHandler.postDelayed({ startListening() }, 200)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    DisposableEffect(context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            isSpeaking = false
                            if (appState != ExamState.FINISHED) {
                                spokenFeedback = "Listening..."
                                startListening()
                            }
                        }
                    }
                    override fun onError(utteranceId: String?) { mainHandler.post { isSpeaking = false } }
                })
                if (hasPermissions) speakText(instructionsText, "TTS_INSTRUCTIONS")
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
            backgroundExecutor.shutdown()
        }
    }

    // --- UI LAYOUT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .clickable {
                if (hasPermissions && appState != ExamState.FINISHED && !isSpeaking) startListening()
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // HEADER with Mic and Live Camera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFFFF5252) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Microphone Status",
                            tint = if (isListening) Color.White else Color(0xFF2ADECD),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // 🔥 LIVE CAMERA PREVIEW FOR GESTURES 🔥
                    if (hasPermissions) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(2.dp, if(detectedGesture.isNotEmpty()) Color(0xFFFFD54F) else Color(0xFF2ADECD), CircleShape)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                            .build()

                                        imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                                            handLandmarker?.let { landmarker ->
                                                val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                                                imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(it.planes[0].buffer) }

                                                val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                                                val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)

                                                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                                                landmarker.detectAsync(mpImage, System.currentTimeMillis())
                                            }
                                            imageProxy.close()
                                        }

                                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                                        } catch (e: Exception) {}
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay detected gesture
                            if (detectedGesture.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (detectedGesture) {
                                            "SUBMIT" -> "✋"
                                            "THUMB_UP" -> "👍"
                                            "THUMB_DOWN" -> "👎"
                                            "SKIP" -> "✊"
                                            else -> detectedGesture
                                        },
                                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = spokenFeedback,
                color = if (isListening) Color.White else Color.LightGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2ADECD),
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724).copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when (appState) {
                        ExamState.INSTRUCTIONS -> "Listen to the instructions carefully. Say 'Start' to begin."
                        ExamState.EARLY_SUBMIT_CONFIRM -> "Are you sure you want to submit early? Show 👍 for Yes, or 👎 for No."
                        else -> currentQuestion
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp),
                    lineHeight = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (appState != ExamState.INSTRUCTIONS && appState != ExamState.EARLY_SUBMIT_CONFIRM) {
                options.forEachIndexed { index, optionText ->
                    val isSelected = selectedOption == index
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(65.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color(0xFFFFD54F) else Color.Transparent,
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        border = BorderStroke(2.dp, if (isSelected) Color(0xFFFFD54F) else Color.DarkGray)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Text(text = optionText, fontSize = 20.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}