package com.example.visionverse.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visionverse.ui.viewmodels.ResultViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBackClick: () -> Unit,
    viewModel: ResultViewModel = viewModel()
) {
    val context = LocalContext.current

    // Fetch data when screen opens
    LaunchedEffect(Unit) {
        viewModel.saveAndFetchResults("", 0, 0)
    }

    val pastResults by viewModel.pastResults.collectAsState()

    // 🔥 SUBJECT FILTERING LOGIC 🔥
    var selectedSubject by remember { mutableStateOf("All") }

    // Dynamically find all subjects the user has taken
    val availableSubjects = remember(pastResults) {
        listOf("All") + pastResults.map { it.subjectId }.distinct()
    }

    // Filter and sort the results based on the selected tab
    val filteredResults = remember(pastResults, selectedSubject) {
        val sorted = pastResults.sortedBy { it.timestamp }
        if (selectedSubject == "All") sorted else sorted.filter { it.subjectId == selectedSubject }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var hasSpoken by remember { mutableStateOf(false) }

    // Animation for the graph line
    val graphProgress = remember { Animatable(0f) }

    // Re-trigger graph animation whenever the filtered data changes
    LaunchedEffect(filteredResults) {
        graphProgress.snapTo(0f) // Reset instantly
        if (filteredResults.isNotEmpty()) {
            graphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
    }

    // --- AI VOICE ANALYSIS ---
    DisposableEffect(context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Trigger voice analysis only once when the screen initially loads with data
    LaunchedEffect(pastResults) {
        if (pastResults.isNotEmpty() && !hasSpoken && tts != null) {
            delay(500)
            val recentScores = pastResults.sortedBy { it.timestamp }.takeLast(3).map { it.score }
            val average = if (recentScores.isNotEmpty()) recentScores.average() else 0.0

            val speechText = when {
                average >= 8.0 -> "Incredible job! Your overall progress is outstanding. Keep up the excellent work."
                average >= 5.0 -> "You are doing well. Consistent practice will help you reach the top scores. Keep going!"
                else -> "I see you are trying hard. Don't worry about the scores right now, focus on learning. You will get there!"
            }
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "TTS_PROGRESS")
            hasSpoken = true
        } else if (pastResults.isEmpty() && !hasSpoken && tts != null) {
            delay(500)
            tts?.speak("You haven't taken any exams yet. Complete an exam to see your progress here.", TextToSpeech.QUEUE_FLUSH, null, "TTS_EMPTY")
            hasSpoken = true
        }
    }

    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF0A0F1A), Color(0xFF16213E))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = { Text("My Progress", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        tts?.stop()
                        onBackClick()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2ADECD), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Performance Trend", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🔥 SLEEK SUBJECT FILTER CHIPS 🔥
                if (availableSubjects.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableSubjects) { subject ->
                            val isSelected = subject == selectedSubject
                            val formattedText = if (subject == "All") "All Subjects"
                            else subject.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                            Surface(
                                color = if (isSelected) Color(0xFF2ADECD).copy(alpha = 0.15f) else Color(0xFF0F1724),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF2ADECD) else Color.DarkGray.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { selectedSubject = subject }
                            ) {
                                Text(
                                    text = formattedText,
                                    color = if (isSelected) Color(0xFF2ADECD) else Color.LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }

                if (filteredResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No exam data available for this selection.", color = Color.LightGray)
                    }
                } else {
                    // 🔥 CUSTOM ANIMATED LINE GRAPH 🔥
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724).copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val maxScore = 10f // Assuming max score is always 10

                                val points = filteredResults.mapIndexed { index, result ->
                                    val x = if (filteredResults.size > 1) {
                                        (index.toFloat() / (filteredResults.size - 1)) * width
                                    } else {
                                        width / 2f
                                    }
                                    val y = height - ((result.score.toFloat() / maxScore) * height)
                                    Offset(x, y)
                                }

                                // Draw Graph Line
                                val path = Path().apply {
                                    points.forEachIndexed { index, point ->
                                        if (index == 0) moveTo(point.x, point.y)
                                        else lineTo(point.x, point.y)
                                    }
                                }

                                // Draw Path with Animation
                                if (points.size > 1) {
                                    importPath(path, graphProgress.value, width, height)
                                }

                                // Draw Data Points
                                points.forEach { point ->
                                    drawCircle(
                                        color = Color(0xFFFFD54F),
                                        radius = 6.dp.toPx() * graphProgress.value,
                                        center = point
                                    )
                                    drawCircle(
                                        color = Color(0xFF0F1724),
                                        radius = 3.dp.toPx() * graphProgress.value,
                                        center = point
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Total Exams Taken", color = Color.Gray, fontSize = 14.sp)
                        Text("${filteredResults.size}", color = Color(0xFFFFD54F), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)

                        Spacer(modifier = Modifier.height(20.dp))

                        val avg = if (filteredResults.isNotEmpty()) filteredResults.map{it.score}.average() else 0.0
                        Text("Average Score", color = Color.Gray, fontSize = 14.sp)
                        Text(String.format(Locale.US, "%.1f / 10", avg), color = Color(0xFF2ADECD), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// Helper to animate the drawing of the path smoothly
fun androidx.compose.ui.graphics.drawscope.DrawScope.importPath(path: Path, progress: Float, width: Float, height: Float) {
    clipRect(right = width * progress) {
        drawPath(
            path = path,
            color = Color(0xFF2ADECD),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}