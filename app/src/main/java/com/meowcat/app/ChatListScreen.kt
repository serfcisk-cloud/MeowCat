package com.meowcat.app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ChatViewItem(
    val chatId: String,
    val nickname: String,
    val photoUrl: String?,
    val lastMessage: String,
    val lastTimestamp: Timestamp?,
    val unreadCount: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUserId = Firebase.auth.currentUser?.uid ?: return

    var chatItems by remember { mutableStateOf<List<ChatViewItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var chatToDelete by remember { mutableStateOf<ChatViewItem?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUserId) {
        db.collection("chats_users")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                coroutineScope.launch {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val users = data["users"] as? List<String> ?: return@mapNotNull null
                        val otherUserId = users.firstOrNull { it != currentUserId } ?: return@mapNotNull null

                        var nickname = ""
                        var photoUrl: String? = null

                        val userData = (data["userData"] as? Map<String, Map<String, Any>>)?.get(otherUserId)
                        nickname = userData?.get("nickname") as? String ?: ""
                        photoUrl = userData?.get("profileImageUrl") as? String

                        if (nickname.isBlank()) {
                            try {
                                val userDoc = db.collection("users").document(otherUserId).get().await()
                                nickname = userDoc.getString("nickname") ?: "Пользователь"
                                photoUrl = userDoc.getString("profileImageUrl")
                            } catch (e: Exception) {
                                nickname = "Пользователь"
                            }
                        }

                        val lastMsg = data["lastMessage"] as? String ?: ""
                        val lastTs = data["lastTimestamp"] as? Timestamp
                        val unread = (data["unreadCount_$currentUserId"] as? Long) ?: 0L

                        ChatViewItem(doc.id, nickname, photoUrl, lastMsg, lastTs, unread)
                    }

                    chatItems = items.sortedByDescending { it.lastTimestamp?.seconds ?: 0L }
                    isLoading = false
                }
            }
    }

    fun formatTime(ts: Timestamp?): String {
        if (ts == null) return ""
        val date = ts.toDate()
        val now = Date()
        val diff = now.time - date.time
        return when {
            diff < 60_000 -> "только что"
            diff < 3_600_000 -> "${diff / 60_000} мин."
            diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            diff < 172_800_000 -> "вчера"
            else -> SimpleDateFormat("d MMM", Locale("ru")).format(date)
        }
    }

    fun deleteChatForMe(chatId: String) {
        val chatRef = db.collection("chats_users").document(chatId)
        chatRef.update("users", FieldValue.arrayRemove(currentUserId))
    }

    if (chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Удалить чат?") },
            text = { Text("Чат будет удалён только у вас. У собеседника он останется.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteChatForMe(chatToDelete!!.chatId)
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { chatToDelete = null }) { Text("Отмена") } }
        )
    }

    Surface(color = Color(0xFF6A1B9A)) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Чаты", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                } else if (chatItems.isEmpty()) {
                    Text(
                        "Нет чатов\nНачните общение в «Ленте»",
                        color = Color.White.copy(0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else {
                    LazyColumn {
                        items(chatItems, key = { it.chatId }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // --- ИСПРАВЛЕНО: Объединены обработчики нажатий ---
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { navController.navigate("${Screen.Chat.route}/${item.chatId}") },
                                            onLongPress = { chatToDelete = item }
                                        )
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.photoUrl ?: R.drawable.cat_logo,
                                    contentDescription = "Аватар",
                                    modifier = Modifier.size(56.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(item.nickname, fontWeight = FontWeight.Medium, color = Color.White)
                                        Text(formatTime(item.lastTimestamp), color = Color.White.copy(0.7f), fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        item.lastMessage,
                                        color = Color.White.copy(0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (item.unreadCount > 0) {
                                    Spacer(Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier.size(24.dp).background(Color(0xFFFFA500), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Divider(color = Color.White.copy(0.1f))
                        }
                    }
                }
            }
        }
    }
}