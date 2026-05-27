package com.example.visionverse.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF0A0F1A), Color(0xFF009688))
    )

    // --- SHARED PREFERENCES SETUP ---
    // This permanently saves the user's toggle choices to their device
    val sharedPreferences = remember { context.getSharedPreferences("VisionVerseSettings", Context.MODE_PRIVATE) }

    var notificationsEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("notifications_enabled", true))
    }
    var soundEffectsEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("sound_effects_enabled", true))
    }
    var darkModeEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("dark_mode_enabled", true))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Custom Top Bar
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // --- ACCOUNT SECTION ---
                SettingsSectionTitle("Account")
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Person,
                        text = "Edit Profile",
                        onClick = onNavigateToProfile
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // 🔥 SECURE FIREBASE PASSWORD RESET 🔥
                    SettingsClickableRow(
                        icon = Icons.Default.Lock,
                        text = "Change Password",
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user?.email != null) {
                                FirebaseAuth.getInstance().sendPasswordResetEmail(user.email!!)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Password reset email sent to ${user.email}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "No email linked to this account.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- PREFERENCES SECTION ---
                SettingsSectionTitle("Preferences")
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Notifications,
                        text = "Push Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { isChecked ->
                            notificationsEnabled = isChecked
                            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
                        }
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon = Icons.Default.VolumeUp,
                        text = "Exam Sound Effects",
                        checked = soundEffectsEnabled,
                        onCheckedChange = { isChecked ->
                            soundEffectsEnabled = isChecked
                            sharedPreferences.edit().putBoolean("sound_effects_enabled", isChecked).apply()
                        }
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon = Icons.Default.DarkMode,
                        text = "Dark Mode",
                        checked = darkModeEnabled,
                        onCheckedChange = { isChecked ->
                            darkModeEnabled = isChecked
                            sharedPreferences.edit().putBoolean("dark_mode_enabled", isChecked).apply()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SUPPORT & ABOUT SECTION ---
                // 🔥 NATIVE WEB BROWSER REDIRECTS 🔥
                SettingsSectionTitle("Support & About")
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.HelpOutline,
                        text = "Help Center & FAQ",
                        onClick = { uriHandler.openUri("https://support.google.com") }
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsClickableRow(
                        icon = Icons.Default.PrivacyTip,
                        text = "Privacy Policy",
                        onClick = { uriHandler.openUri("https://policies.google.com/privacy") }
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsClickableRow(
                        icon = Icons.Default.Description,
                        text = "Terms of Service",
                        onClick = { uriHandler.openUri("https://policies.google.com/terms") }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // App Version
                Text(
                    text = "VisionVerse v1.0.0",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- REUSABLE UI COMPONENTS FOR SETTINGS ---

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF2ADECD),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724).copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsClickableRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun SettingsSwitchRow(icon: ImageVector, text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0F1724),
                checkedTrackColor = Color(0xFF2ADECD),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}