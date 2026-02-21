package com.meowcat.app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

// Используем уже существующий data class Friend, т.к. структура та же

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val myId = currentUser?.uid ?: ""

    var likers by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(myId) {
        if (myId.isNotBlank()) {
            isLoading = true
            try {
                // Ищем всех пользователей, у которых в массиве 'likes' есть наш ID
                val snapshot = db.collection("users")
                    .whereArrayContains("likes", myId)
                    .get()
                    .await()

                val usersWhoLikedMe = snapshot.documents.mapNotNull { doc ->
                    Friend(
                        uid = doc.id,
                        nickname = doc.getString("nickname"),
                        profileImageUrl = doc.getString("profileImageUrl")
                    )
                }
                likers = usersWhoLikedMe

            } catch (e: Exception) {
                Log.e("NotificationsScreen", "Error fetching likers", e)
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Surface(color = Color(0xFF6A1B9A), modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Кто меня лайкнул", color = Color.White) },
                    navigationIcon = { 
                        IconButton(onClick = { navController.popBackStack() }) { 
                            Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White) 
                        } 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (likers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Вас пока никто не лайкнул", color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(likers) { liker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("${Screen.UserProfile.route}/${liker.uid}") }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = liker.profileImageUrl ?: R.drawable.cat_logo,
                                contentDescription = null, 
                                modifier = Modifier.size(50.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(liker.nickname ?: "Аноним", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Divider(color = Color.White.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}