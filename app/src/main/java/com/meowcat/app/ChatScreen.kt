package com.meowcat.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val createdAt: Timestamp? = null,
    var messageId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, chatId: String) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUserId = auth.currentUser?.uid.orEmpty()
    
    val otherUserId = remember(chatId, currentUserId) {
        chatId.split("-").firstOrNull { it != currentUserId } ?: ""
    }

    // --- ДОБАВЛЕНО: Сброс счетчика непрочитанных при входе в чат ---
    LaunchedEffect(chatId, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("chats_users").document(chatId)
                .update("unreadCount_$currentUserId", 0)
                .addOnFailureListener { e ->
                    Log.e("ChatScreen", "Failed to reset unread count", e)
                }
        }
    }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    val listState = rememberLazyListState()

    fun sendMessage(text: String? = null, imageUrl: String? = null) {
        if (text.isNullOrBlank() && imageUrl.isNullOrBlank()) return
        if (currentUserId.isBlank() || otherUserId.isBlank()) return

        val batch = db.batch()
        val newMessageRef = db.collection("chats_users/$chatId/messages").document()
        
        val messageData = hashMapOf<String, Any?>(
            "senderId" to currentUserId,
            "createdAt" to FieldValue.serverTimestamp(),
            "users" to listOf(currentUserId, otherUserId),
            "text" to text,
            "imageUrl" to imageUrl
        )
        batch.set(newMessageRef, messageData)

        val chatDocRef = db.document("chats_users/$chatId")
        val chatPreviewData = mapOf(
            "lastMessage" to (text ?: "Изображение"),
            "lastTimestamp" to FieldValue.serverTimestamp(),
            "unreadCount_$otherUserId" to FieldValue.increment(1),
            "users" to listOf(currentUserId, otherUserId)
        )
        batch.set(chatDocRef, chatPreviewData, SetOptions.merge())

        batch.commit().addOnSuccessListener {
            if (text != null) inputText = ""
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun uploadImageToChat(uri: Uri) {
        isUploading = true
        MediaManager.get().upload(uri)
            .option("folder", "chat_images")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    sendMessage(imageUrl = imageUrl)
                    isUploading = false
                }
                override fun onError(requestId: String, error: ErrorInfo?) {
                    Toast.makeText(context, "Ошибка загрузки: ${error?.description}", Toast.LENGTH_SHORT).show()
                    isUploading = false
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToChat(it) }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickImageLauncher.launch("image/*")
    }

    val attachImage = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    val deleteMessage: (Message) -> Unit = { msg ->
        msg.messageId?.let { id ->
            db.collection("chats_users/$chatId/messages").document(id).delete()
        }
    }

    DisposableEffect(chatId) {
        val listener = db.collection("chats_users/$chatId/messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Message>()?.copy(messageId = doc.id)
                } ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        containerColor = Color(0xFF6A1B9A),
        topBar = {
            TopAppBar(
                title = { Text("Чат", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6A1B9A))
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(25.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = attachImage, enabled = !isUploading) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFE65100))
                    } else {
                        Icon(Icons.Default.AttachFile, "Прикрепить", tint = Color(0xFFE65100))
                    }
                }

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Сообщение...") },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray
                    ),
                    singleLine = false,
                    maxLines = 5
                )

                IconButton(
                    onClick = { sendMessage(text = inputText.trim()) },
                    enabled = inputText.isNotBlank() && !isUploading
                ) {
                    Icon(Icons.Default.Send, "Отправить", tint = if (inputText.isNotBlank()) Color(0xFFE65100) else Color.Gray)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF6A1B9A))
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(messages) { index, message ->
                val prev = messages.getOrNull(index - 1)
                val showDate = prev?.createdAt?.toDate()?.let {
                    !isSameDay(it.time, message.createdAt?.toDate()?.time ?: 0L)
                } ?: true
                if (showDate) {
                    DateHeader(message.createdAt?.toDate()?.time ?: 0L)
                }

                MessageBubble(
                    message = message,
                    isMyMessage = message.senderId == currentUserId,
                    onLongClick = {
                        if (message.senderId == currentUserId) messageToDelete = message
                    }
                )
            }
        }
    }

    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Удалить сообщение?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteMessage(messageToDelete!!)
                    messageToDelete = null
                }) { Text("Удалить", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun MessageBubble(message: Message, isMyMessage: Boolean, onLongClick: () -> Unit) {
    val bubbleColor = if (isMyMessage) Color(0xFFFFD180) else Color(0xFFD1C4E9)
    val alignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongClick() }) }
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            message.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Фото",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(4.dp))
            }
            if (!message.text.isNullOrEmpty()) {
                Text(message.text, fontSize = 16.sp, color = Color.Black)
            }
            Text(
                text = message.createdAt?.toDate()?.let {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                } ?: "",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun DateHeader(timestamp: Long) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(
            text = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(timestamp)),
            color = Color.White.copy(0.7f),
            fontSize = 13.sp,
            modifier = Modifier
                .background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
