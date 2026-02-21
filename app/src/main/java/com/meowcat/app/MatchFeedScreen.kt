package com.meowcat.app

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class MatchResult(val uid: String, val nickname: String?, val profileImageUrl: String?, val age: Int?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchFeedScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val user = auth.currentUser

    var isLoading by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) } 
    var showResults by remember { mutableStateOf(false) } 
    var matches by remember { mutableStateOf<List<MatchResult>>(emptyList()) }
    var myProfile by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    val totalUnreadCount by rememberTotalUnreadCount()

    LaunchedEffect(user) {
        if (user != null) {
            isLoading = true
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        myProfile = document.data ?: emptyMap()
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    val findMatches: () -> Unit = {
        user?.uid?.let { myId ->
            if (myProfile.isNotEmpty()) {
                val seekingGender = myProfile["seeking"] as? String
                val seekingCountry = myProfile["seeking_country"] as? String
                val minAge = (myProfile["seeking_age_min"] as? Double)?.toInt()
                val maxAge = (myProfile["seeking_age_max"] as? Double)?.toInt()
                val myBlockedUsers = myProfile["blockedUsers"] as? List<String> ?: emptyList()

                if (seekingCountry.isNullOrBlank()) {
                    Toast.makeText(context, "Выберите страну для поиска в настройках местоположения", Toast.LENGTH_LONG).show()
                    return@let
                }
                
                isLoading = true
                var query: Query = db.collection("users").whereEqualTo("my_country", seekingCountry)

                if (seekingGender != null) {
                    query = query.whereEqualTo("gender", seekingGender)
                }

                query.get().addOnSuccessListener { snapshot ->
                    val results = snapshot.documents.mapNotNull { doc ->
                        val docId = doc.id
                        if (docId == myId || myBlockedUsers.contains(docId)) return@mapNotNull null

                        val theirBlocked = doc.get("blockedUsers") as? List<String> ?: emptyList()
                        if (theirBlocked.contains(myId)) return@mapNotNull null

                        val age = (doc.getDouble("age"))?.toInt()
                        val ageMatch = age != null && minAge != null && maxAge != null && age in minAge..maxAge

                        if (ageMatch) {
                            MatchResult(
                                uid = docId,
                                nickname = doc.getString("nickname"),
                                profileImageUrl = doc.getString("profileImageUrl"),
                                age = age
                            )
                        } else null
                    }
                    matches = results.sortedBy { it.age }
                    showResults = true
                    isLoading = false
                }.addOnFailureListener { e ->
                    Log.e("FIRESTORE_QUERY", "Error finding matches: ", e)
                    Toast.makeText(context, "Ошибка поиска. Попробуйте позже.", Toast.LENGTH_LONG).show()
                    isLoading = false 
                }
            } 
        }
    }

    val onSignOut: () -> Unit = {
        auth.signOut()
        navController.navigate(Screen.Main.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    Surface(color = Color(0xFF6A1B9A), modifier = Modifier.fillMaxSize()) { 
        Scaffold(
            topBar = {
                 TopAppBar(
                    title = { Text(if (showResults) "Результаты поиска" else "Лента", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        if (showResults) {
                            IconButton(onClick = { showResults = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
                            }
                        }
                    },
                    // --- ИСПРАВЛЕНО: Возвращен блок actions с шестеренкой ---
                    actions = {
                        if (!showResults) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Color.White)
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(text = { Text("Редактировать профиль") }, onClick = { navController.navigate(Screen.Home.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Управление фото") }, onClick = { navController.navigate(Screen.PhotoManager.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Управление местоположением") }, onClick = { navController.navigate(Screen.CityManager.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Черный список") }, onClick = { navController.navigate(Screen.Blacklist.route); showMenu = false })
                                    Divider()
                                    DropdownMenuItem(text = { Text("Выйти") }, onClick = onSignOut)
                                }
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (!showResults) {
                     Row(
                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha=0.1f), RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = myProfile["profileImageUrl"] as? String ?: R.drawable.cat_logo,
                            contentDescription = "Мой аватар",
                            modifier = Modifier.size(50.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(myProfile["nickname"] as? String ?: "Анонимный котик", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (showResults) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else if (matches.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("По вашим критериям никого не найдено 😿", color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Попробуйте изменить параметры поиска", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(matches) { match ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha=0.2f), RoundedCornerShape(16.dp))
                                        .clickable { navController.navigate("${Screen.UserProfile.route}/${match.uid}") }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = match.profileImageUrl ?: R.drawable.cat_logo, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(match.nickname ?: "Котик", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                        Text("Возраст: ${match.age}", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                     if (isLoading) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                     } else {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardButton(text = "Найти", icon = Icons.Default.Search, onClick = findMatches)
                                DashboardButton(text = "Друзья", icon = Icons.Default.SupervisorAccount, onClick = { navController.navigate(Screen.Friends.route) })
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardButton(
                                    text = "Чаты", 
                                    icon = Icons.Default.Message, 
                                    badgeCount = totalUnreadCount,
                                    onClick = { navController.navigate(Screen.ChatList.route) }
                                )
                                DashboardButton(text = "Уведомления", icon = Icons.Default.Notifications, onClick = { navController.navigate(Screen.Notifications.route) })
                            }
                        }
                    }
                }
            }
        }
    }
} 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardButton(text: String, icon: ImageVector, onClick: () -> Unit, badgeCount: Int = 0) {
    BadgedBox(
        badge = {
            if (badgeCount > 0) {
                Badge(modifier = Modifier.offset(x = (-8).dp, y = 8.dp)) { 
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        fontSize = 14.sp
                    ) 
                }
            }
        }
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF90)),
            modifier = Modifier.size(140.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(imageVector = icon, contentDescription = text, tint = Color.Black.copy(alpha = 0.8f), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = text, color = Color.Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}