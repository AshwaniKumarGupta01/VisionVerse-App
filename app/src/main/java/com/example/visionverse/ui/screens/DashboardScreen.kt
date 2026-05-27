package com.example.visionverse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.visionverse.ui.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, vm: AuthViewModel) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var userName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showProfileMenu by remember { mutableStateOf(false) }

    // Check Auth State and Fetch Data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else {
            try {
                val snapshot = db.collection("users").document(currentUser.uid).get().await()
                userName = snapshot.getString("firstName") ?: "User"
                userEmail = snapshot.getString("email") ?: currentUser.email ?: ""
            } catch (e: Exception) {
                userName = "User"
            } finally {
                isLoading = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f),
                drawerContainerColor = Color(0xFF121212),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userName.isNotEmpty()) userName.first().uppercase() else "U",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(userEmail, color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(30.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(10.dp))

                    DrawerItem("Dashboard", Icons.Default.Dashboard, true) {
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem("My Results", Icons.Default.Assessment) {
                        scope.launch { drawerState.close() }
                        navController.navigate("results/8/10")
                    }
                    DrawerItem("Settings", Icons.Default.Settings) {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // ✅ FIXED LOGOUT: Clears ViewModel State to stop blinking
                    DrawerItem("Logout", Icons.Default.Logout) {
                        scope.launch {
                            drawerState.close()
                            vm.clearAuthStates() // CRITICAL: Stop the blinking loop
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0A0F1A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = { Text("VISION VERSE", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Notes, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showProfileMenu = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(30.dp))
                        }
                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Profile", color = Color.White) },
                                onClick = {
                                    showProfileMenu = false
                                    navController.navigate("profile")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color.White) },
                                onClick = {
                                    showProfileMenu = false
                                    vm.clearAuthStates() // CRITICAL: Stop the blinking loop
                                    auth.signOut()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A0F1A), Color(0xFF16213E))))
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = ""
                )

                Text(
                    text = if (isLoading) "Syncing data..." else "Welcome back, $userName! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.scale(scale)
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text("Active Scheduled Exams", fontSize = 18.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))

                // 🔥 UPDATED: Passing the exact Firebase subject ID
                ExamCard("Android Development", "50 mins", Icons.Default.Android, "android_development", navController)
                ExamCard("Artificial Intelligence", "50 mins", Icons.Default.Psychology, "artificial_intelligence", navController)
                ExamCard("Database Management", "50 mins", Icons.Default.Storage, "database_management", navController)
            }
        }
    }
}

@Composable
fun DrawerItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0x22FFD54F) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color(0xFFFFD54F) else Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = if (selected) Color(0xFFFFD54F) else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// 🔥 UPDATED: Added 'subjectId' parameter
@Composable
fun ExamCard(title: String, duration: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subjectId: String, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(duration, color = Color.Gray, fontSize = 13.sp)
                }
            }
            Button(
                // 🔥 UPDATED: Passes the dynamic subjectId to the route
                onClick = { navController.navigate("exam_screen/$subjectId") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Start", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}