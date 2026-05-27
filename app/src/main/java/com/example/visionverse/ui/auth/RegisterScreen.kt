package com.example.visionverse.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    vm: AuthViewModel,
    onLoginClick: () -> Unit,
    onRegisterSuccess: () -> Unit = {}
) {

    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF0A0F1A), Color(0xFF009688))
    )

    val registerState by vm.registerState.collectAsState(initial = UiState.Idle)

    LaunchedEffect(registerState) {
        when (registerState) {
            is UiState.Success -> {
                delay(800)
                onRegisterSuccess()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724)),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🔥 Animated Logo
                AnimatedAppLogo(
                    size = androidx.compose.ui.unit.Dp(110f),
                    glowColor = Color(0xFF009688)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                AuthInputField(
                    label = "First Name",
                    value = vm.firstName.value,
                    onChange = { vm.firstName.value = it }
                )

                AuthInputField(
                    label = "Last Name",
                    value = vm.lastName.value,
                    onChange = { vm.lastName.value = it }
                )

                AuthInputField(
                    label = "Email",
                    value = vm.email.value,
                    onChange = { vm.email.value = it }
                )

                AuthInputField(
                    label = "Phone Number",
                    value = vm.phone.value,
                    onChange = { vm.phone.value = it }
                )

                AuthPasswordField(
                    label = "Password",
                    value = vm.password.value,
                    onChange = { vm.password.value = it }
                )

                AuthPasswordField(
                    label = "Confirm Password",
                    value = vm.confirmPassword.value,
                    onChange = { vm.confirmPassword.value = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (registerState) {
                    is UiState.Loading -> {
                        Text(
                            text = "Registering...",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }

                    is UiState.Error -> {
                        Text(
                            text = (registerState as UiState.Error).message,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }

                    is UiState.Success -> {
                        val message =
                            if (vm.successMessage.value.isNotBlank())
                                vm.successMessage.value
                            else
                                (registerState as UiState.Success).message

                        Text(
                            text = message,
                            color = Color.Green,
                            fontSize = 14.sp
                        )
                    }

                    else -> {
                        if (vm.errorMessage.value.isNotBlank()) {
                            Text(
                                text = vm.errorMessage.value,
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { vm.register() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                ) {
                    Text(
                        text = "Register",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val interaction = remember { MutableInteractionSource() }

                TextButton(
                    onClick = onLoginClick,
                    interactionSource = interaction
                ) {
                    Text(
                        text = "Already have an account? Login",
                        color = Color.White
                    )
                }
            }
        }
    }
}