package com.example.visionverse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer // 🔥 NEW IMPORT FOR GPU SMOOTHNESS
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visionverse.ui.viewmodels.ResultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ResultScreen(
    subjectId: String,
    score: Int,
    totalQuestions: Int,
    onBackToHome: () -> Unit,
    onRetakeExam: () -> Unit,
    onViewProgress: () -> Unit,
    viewModel: ResultViewModel = viewModel()
) {
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF0A0F1A), Color(0xFF16213E), Color(0xFF009688))
    )

    val pastResults by viewModel.pastResults.collectAsState()
    val topScores by viewModel.topScores.collectAsState()

    // 🔥 THE DEFINITIVE DUPLICATE FIX 🔥
    val uniquePastResults = remember(pastResults) {
        pastResults.filterIndexed { index, current ->
            val isDuplicateOfPrevious = pastResults.take(index).any { previous ->
                current.subjectId == previous.subjectId && abs(current.timestamp - previous.timestamp) < 15000
            }
            !isDuplicateOfPrevious
        }
    }

    var hasSavedToDatabase by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasSavedToDatabase && subjectId != "history") {
            viewModel.saveAndFetchResults(subjectId, score, totalQuestions)
            hasSavedToDatabase = true
        } else if (subjectId == "history" && !hasSavedToDatabase) {
            viewModel.saveAndFetchResults("", 0, 0)
            hasSavedToDatabase = true
        }
    }

    val percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions.toFloat()) else 0f

    val (feedbackText, feedbackColor) = when {
        percentage >= 0.8f -> "Outstanding!" to Color(0xFF2ADECD)
        percentage >= 0.5f -> "Good Effort!" to Color(0xFFFFD54F)
        else -> "Needs Practice" to Color(0xFFFF5252)
    }

    // --- GPU ACCELERATED ANIMATIONS (BUTTERY SMOOTH) ---
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50) // Micro-delay ensures layout is drawn before animating
        startAnimation = true
    }

    // Circular Progress Animation
    val currentPercentage by animateFloatAsState(
        targetValue = if (startAnimation) percentage else 0f,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "progress_animation"
    )

    // Top Section (Current Score) Fade & Glide
    val topSectionAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "top_alpha"
    )
    val topSectionOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 80f, // Glides up 80 pixels
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "top_offset"
    )

    // Bottom Section (History & Top Scores) Fade & Glide - Slightly delayed for a cascading feel
    val bottomSectionAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200, easing = LinearOutSlowInEasing),
        label = "bottom_alpha"
    )
    val bottomSectionOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 80f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200, easing = LinearOutSlowInEasing),
        label = "bottom_offset"
    )

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 🔥 TOP SECTION (GPU RENDERED LAYER)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = topSectionAlpha
                        translationY = topSectionOffset
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (subjectId != "history") {
                    Text(
                        text = "Exam Results",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724).copy(alpha = 0.85f)),
                        elevation = CardDefaults.cardElevation(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = feedbackText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = feedbackColor
                            )

                            Spacer(modifier = Modifier.height(35.dp))

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.DarkGray.copy(alpha = 0.3f),
                                    strokeWidth = 14.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                CircularProgressIndicator(
                                    progress = { currentPercentage },
                                    modifier = Modifier.fillMaxSize(),
                                    color = feedbackColor,
                                    strokeWidth = 14.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(currentPercentage * 100).toInt()}%",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$score / $totalQuestions",
                                        fontSize = 16.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(35.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem(icon = Icons.Default.CheckCircle, value = "$score", label = "Correct", color = Color(0xFF2ADECD))
                                StatItem(icon = Icons.Default.Star, value = "+${score * 10}", label = "Points", color = Color(0xFFFFD54F))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Dashboard", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onRetakeExam,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake Exam", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onViewProgress,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF2ADECD).copy(alpha = 0.05f),
                            contentColor = Color(0xFF2ADECD)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF2ADECD)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2ADECD))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View My Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ADECD))
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        IconButton(onClick = onBackToHome) {
                            Icon(Icons.Default.Home, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Text("My Results", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = onViewProgress,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ADECD)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Graphical Progress", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // 🔥 BOTTOM SECTION (HISTORY & STATS - GPU RENDERED)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = bottomSectionAlpha
                        translationY = bottomSectionOffset
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (topScores.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD54F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Top Scores by Subject", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(topScores.entries.toList()) { entry ->
                            val formattedSubjectName = entry.key.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E).copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .width(170.dp)
                                    .height(120.dp),
                                border = BorderStroke(1.dp, Color(0xFF2ADECD).copy(alpha = 0.3f)),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(formattedSubjectName, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("${entry.value} / 10", color = Color(0xFF2ADECD), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                if (uniquePastResults.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFFFD54F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recent Exams", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    uniquePastResults.forEach { result ->
                        val formattedSubjectName = result.subjectId.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        val dateString = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(result.timestamp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(formattedSubjectName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(dateString, color = Color.Gray, fontSize = 13.sp)
                                }
                                Text("${result.score}/${result.total}", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
    }
}