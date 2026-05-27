package com.example.visionverse.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.visionverse.ui.auth.*
import com.example.visionverse.ui.screens.*

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    // Create ViewModel
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(navController)
        }

        composable("login") {
            LoginScreen(
                vm = authViewModel,
                onRegisterClick = {
                    navController.navigate("signup")
                },
                onLoginSuccess = {
                    navController.navigate("dashboard")
                }
            )
        }

        composable("signup") {
            RegisterScreen(
                vm = authViewModel,
                onLoginClick = {
                    navController.navigate("login")
                },
                onRegisterSuccess = {
                    navController.navigate("login")
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(navController, authViewModel)
        }

        composable(
            route = "exam_screen/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Extract the subjectId from the route
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "android_development"

            ExamScreen(
                subjectId = subjectId,
                onExamComplete = { score, total ->
                    // 🔥 CRITICAL FIX: Pass the subjectId to the results screen using a query parameter
                    navController.navigate("results/$score/$total?subjectId=$subjectId") {
                        // Pop the exam screen off the backstack so they can't hit the back button into it
                        popUpTo("dashboard") { inclusive = false }
                    }
                }
            )
        }

        // 🔥 CRITICAL FIX: Updated route to accept an optional subjectId
        composable(
            route = "results/{score}/{total}?subjectId={subjectId}",
            arguments = listOf(
                navArgument("score") { type = NavType.StringType; defaultValue = "0" },
                navArgument("total") { type = NavType.StringType; defaultValue = "10" },
                navArgument("subjectId") { type = NavType.StringType; nullable = true; defaultValue = "history" }
            )
        ) { backStackEntry ->
            // Safely grab the strings and convert them to Ints.
            val scoreString = backStackEntry.arguments?.getString("score") ?: "0"
            val totalString = backStackEntry.arguments?.getString("total") ?: "10"

            // Extract the subject ID (defaults to "history" if opened from the drawer)
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "history"

            val score = scoreString.toIntOrNull() ?: 0
            val total = totalString.toIntOrNull() ?: 10

            ResultScreen(
                subjectId = subjectId, // 🔥 Passed the missing parameter here!
                score = score,
                totalQuestions = total,
                onBackToHome = {
                    // Navigate to dashboard and clear exam/results from back history
                    navController.navigate("dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRetakeExam = {
                    // Navigate to dashboard so they can select a subject again
                    navController.navigate("dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onViewProgress = { // 🔥 NAVIGATION TO PROGRESS SCREEN
                    navController.navigate("progress")
                }
            )
        }

        // 🔥 NEW PROGRESS ROUTE
        composable("progress") {
            ProgressScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable("profile") {
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                vm = profileViewModel,
                onLogoutClick = {
                    // Clear the login success state so it doesn't auto-login and blink!
                    authViewModel.clearAuthStates()

                    // Navigate back to login and completely clear the back history
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}