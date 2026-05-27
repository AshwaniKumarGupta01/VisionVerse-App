package com.example.visionverse.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
//import com.example.visionverse.R // Ensure this import is correct
import kotlinx.coroutines.delay
import com.ashutoshparahu.visionverse.R


// Define our specific color palette
val CerebralTeal = Color(0xFF2ADECD)
val DeepBackground = Color(0xFF0A0F1A) // Dark, almost black background

@Composable
fun SplashScreen(navController: NavController) {

    // Simple state to control when the logo and text become visible (fade in)
    var isContentVisible by remember { mutableStateOf(false) }

    // Simple state to control when the background dissolves (fade out to the next screen color)
    var isDissolving by remember { mutableStateOf(false) }

    // Control background gradient transition
    val backgroundTransition = updateTransition(targetState = isDissolving, label = "BackgroundTransition")

    // The ending color of the gradient transitions from DeepBackground to Off-White
    val bgGradientColorEnd by backgroundTransition.animateColor(
        label = "BgGradientEnd",
        transitionSpec = { tween(durationMillis = 1500, easing = LinearEasing) }
    ) { dissolving ->
        if (dissolving) Color(0xFFF5F5F5) else DeepBackground
    }

    // This block orchestrates the simple fade sequence and navigation.
    LaunchedEffect(Unit) {
        // --- The Sequence Begins ---

        // PHASE 1: Screen fades in from white to the Deep Teal/Black gradient.
        // It starts calm and minimal.
        delay(1000)

        // PHASE 2: The static PNG logo and "VISIONVERSE" typography arrive together.
        // As requested, they fade in cleanly without motion.
        isContentVisible = true
        delay(3500) // Sustain visibility for the user to read/see.

        // PHASE 3: Prepare to transition.
        isDissolving = true
        // Allow the content to be visible briefly before the text fades out.
        delay(1000)

        // We toggle isContentVisible back to false to trigger the typography's fade-out exit transition.
        isContentVisible = false
        delay(500) // Allow dissolve and typography fade-out time.

        // --- Navigation ---
        // Pop the splash screen from the backstack to white (the next screen).
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Brush-based background transition
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CerebralTeal, bgGradientColorEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // THE ICON: Static PNG Image
            // It fades in and out with the same logic as the text below it.
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(animationSpec = tween(1000, easing = LinearOutSlowInEasing)),
                // Quick dissolve when transitioning out
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Image(
                    // References your drawable asset: res/drawable/visionverse_icon.png
                    painter = painterResource(id = R.drawable.cropped_circle2),
                    contentDescription = "VisionVerse Static Logo",
                    modifier = Modifier
                        .size(200.dp) // Maintain size for visual balance
                        .padding(bottom = 16.dp),
                    // High-quality content scaling
                    contentScale = ContentScale.Fit
                )
            }

            // THE TYPOGRAPHY: Static Arrival
            AnimatedVisibility(
                visible = isContentVisible,
                // A smooth fade-in arrival (matches the logo)
                enter = fadeIn(animationSpec = tween(1000, easing = LinearOutSlowInEasing)),
                // Quick dissolve when transitioning
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VISION",
                        color = CerebralTeal, // Confident, intellectual color
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "VERSE",
                        color = CerebralTeal, // Same color, contrasting weight
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraLight,
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}