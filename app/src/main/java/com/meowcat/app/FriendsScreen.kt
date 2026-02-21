package com.meowcat.app

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val myId = currentUser?.uid ?: ""

    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(myId) {
        if (myId.isNotBlank()) {
            isLoading = true
            try {
                // 1. Получаем список ID тех, кого лайкнул Я.
                val myLikesSnapshot = db.collection("users").document(myId).get().await()
                val myLikes = myLikesSnapshot.get("likes") as? List<String> ?: emptyList()

                if (myLikes.isEmpty()) {
                    isLoading = false
                    return@LaunchedEffect
                }

                // 2. Ищем среди них тех, кто лайкнул МЕНЯ в ответ.
                val friendsList = mutableListOf<Friend>()
                myLikes.forEach { friendId ->
                    val friendDoc = db.collection("users").document(friendId).get().await()
                    if (friendDoc.exists()) {
                        val theirLikes = friendDoc.get("likes") as? List<String> ?: emptyList()
                        if (theirLikes.contains(myId)) {
                            friendsList.add(
                                Friend(
                                    uid = friendDoc.id,
                                    nickname = friendDoc.getString("nickname"),
                                    profileImageUrl = friendDoc.getString("profileImageUrl")
                                )
                            )
                        }
                    }
                }
                friends = friendsList

            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Surface(color = Color(0xFF6A1B9A), modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Мои друзья", color = Color.White) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
            } else if (friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("У вас пока нет друзей", color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val chatId = if (myId > friend.uid) "$myId-${friend.uid}" else "${friend.uid}-$myId"
                                    navController.navigate("${Screen.Chat.route}/$chatId")
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(model = friend.profileImageUrl ?: R.drawable.cat_logo, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(friend.nickname ?: "Аноним", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Divider(color = Color.White.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}