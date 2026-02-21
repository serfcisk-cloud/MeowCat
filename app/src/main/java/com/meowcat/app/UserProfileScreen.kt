package com.meowcat.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class UserProfile(
    val nickname: String? = null,
    val profileImageUrl: String? = null,
    val aboutMe: String? = null,
    val age: Int? = null,
    val height: Int? = null,
    val gender: String? = null,
    val hobbies: List<String> = emptyList(),
    val photoUrls: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLiking by remember { mutableStateOf(false) }
    var iHaveLikedThem by remember { mutableStateOf(false) }
    var isMatch by remember { mutableStateOf(false) }
    var viewingPhotoUrl by remember { mutableStateOf<String?>(null) }
    var showGalleryDialog by remember { mutableStateOf(false) }
    var isFetchingPhotos by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    val hobbyEmojiMap = remember { allHobbies.associateBy({ it.name }, { it.emoji }) }

    val allUserPhotos = remember(userProfile) {
        (listOfNotNull(userProfile?.profileImageUrl) + (userProfile?.photoUrls ?: emptyList())).distinct()
    }

    fun checkMatchAndStatus() {
        val myId = currentUser?.uid
        if (myId == null) {
            isLoading = false
            return
        }

        isLoading = true
        val theirDocRef = db.collection("users").document(userId)
        val myDocRef = db.collection("users").document(myId)

        theirDocRef.get().addOnSuccessListener { theirDoc ->
            if (theirDoc != null && theirDoc.exists()) {
                userProfile = UserProfile(
                    nickname = theirDoc.getString("nickname"),
                    profileImageUrl = theirDoc.getString("profileImageUrl"),
                    aboutMe = theirDoc.getString("about_me"),
                    age = theirDoc.getDouble("age")?.toInt(),
                    height = theirDoc.getDouble("height")?.toInt(),
                    gender = theirDoc.getString("gender"),
                    hobbies = theirDoc.get("hobbies") as? List<String> ?: emptyList(),
                    photoUrls = theirDoc.get("photo_gallery") as? List<String> ?: emptyList()
                )
                val theirLikes = theirDoc.get("likes") as? List<String> ?: emptyList()

                myDocRef.get().addOnSuccessListener { myDoc ->
                    val myLikes = myDoc.get("likes") as? List<String> ?: emptyList()

                    iHaveLikedThem = myLikes.contains(userId)
                    val theyHaveLikedMe = theirLikes.contains(myId)

                    isMatch = iHaveLikedThem && theyHaveLikedMe

                    isLoading = false
                }.addOnFailureListener { isLoading = false }
            } else {
                isLoading = false
            }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(userId, currentUser) {
        checkMatchAndStatus()
    }

    val onLikeClick: () -> Unit = {
        val myId = currentUser?.uid
        if (myId != null && !isLiking && !iHaveLikedThem) {
            isLiking = true
            val myDocRef = db.collection("users").document(myId)

            myDocRef.update("likes", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    iHaveLikedThem = true
                    db.collection("users").document(userId).get().addOnSuccessListener { theirDoc ->
                        val theirLikes = theirDoc.get("likes") as? List<String> ?: emptyList()
                        if (theirLikes.contains(myId)) {
                            isMatch = true
                            Toast.makeText(context, "Это мэтч! 🎉", Toast.LENGTH_LONG).show()

                            val matchId = if (myId > userId) "$myId-$userId" else "$userId-$myId"
                            val matchData = mapOf("users" to listOf(myId, userId), "timestamp" to FieldValue.serverTimestamp())
                            db.collection("matches").document(matchId).set(matchData)
                        }
                    }
                }
                .addOnCompleteListener { isLiking = false }
        }
    }

    fun showGallery() {
        if (allUserPhotos.isNotEmpty()) {
            showGalleryDialog = true
        }
    }

    val onBlockClick: () -> Unit = {
        val myId = currentUser?.uid
        if (myId != null) {
            val matchId = if (myId > userId) "$myId-$userId" else "$userId-$myId"
            db.collection("matches").document(matchId).delete() 
            db.collection("users").document(myId).update("blockedUsers", FieldValue.arrayUnion(userId))
            db.collection("users").document(myId).update("likes", FieldValue.arrayRemove(userId))
            db.collection("users").document(userId).update("likes", FieldValue.arrayRemove(myId))
            Toast.makeText(context, "Пользователь заблокирован", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Заблокировать пользователя?") },
            text = { Text("Вы уверены, что хотите заблокировать этого пользователя? Вы больше не будете видеть друг друга в поиске и не сможете общаться.") },
            confirmButton = { Button(onClick = { onBlockClick(); showBlockDialog = false }) { Text("Заблокировать") } },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Отмена") } }
        )
    }

    if (viewingPhotoUrl != null) {
        Dialog(onDismissRequest = { viewingPhotoUrl = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { viewingPhotoUrl = null }, contentAlignment = Alignment.Center) {
                AsyncImage(model = viewingPhotoUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().clickable { }, contentScale = ContentScale.Fit)
            }
        }
    }

    if (showGalleryDialog) {
        Dialog(onDismissRequest = { showGalleryDialog = false }) {
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                if (allUserPhotos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Нет загруженных фото", color = Color.Black)
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp)) {
                        items(allUserPhotos) { photoUrl ->
                            AsyncImage(model = photoUrl, contentDescription = "Gallery photo", modifier = Modifier.padding(4.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { viewingPhotoUrl = photoUrl }, contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
        }
    }

    Surface(color = Color(0xFF6A1B9A), modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(userProfile?.nickname ?: "Профиль", color = Color.White) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White) } },
                    actions = {
                        IconButton(onClick = { checkMatchAndStatus() }) {
                            Icon(Icons.Default.Refresh, "Обновить", tint = Color.White)
                        }
                        IconButton(onClick = { showBlockDialog = true }) {
                            Icon(Icons.Default.Block, "Заблокировать", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                when {
                    isMatch -> {
                        Button(onClick = {
                            val myId = currentUser?.uid
                            if (myId != null) {
                                val chatId = if (myId > userId) "$myId-$userId" else "$userId-$myId"
                                navController.navigate("${Screen.Chat.route}/$chatId")
                            }
                        }) {
                            Icon(Icons.Default.Message, null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Написать сообщение")
                        }
                    }
                    iHaveLikedThem -> {
                        Button(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Favorite, null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Лайк отправлен")
                        }
                    }
                    else -> {
                        Button(onClick = onLikeClick, enabled = !isLiking) {
                            Icon(Icons.Default.Favorite, null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(if (isLiking) "Отправка..." else "Лайк")
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
            } else if (userProfile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Не удалось загрузить профиль", color = Color.White) }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(model = userProfile?.profileImageUrl ?: R.drawable.cat_logo, contentDescription = "Аватар", modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.Gray.copy(alpha=0.1f)).clickable { viewingPhotoUrl = userProfile?.profileImageUrl }, contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        userProfile?.nickname?.let { Text(it, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White) }
                        userProfile?.age?.let { Text(", $it", style = MaterialTheme.typography.headlineMedium, color = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        userProfile?.height?.let { Text("Рост: $it см", color = Color.White.copy(alpha = 0.8f)) }
                        if (userProfile?.height != null && userProfile?.gender != null) { Text(" | ", color = Color.White.copy(alpha = 0.8f)) }
                        userProfile?.gender?.let { Text(it, color = Color.White.copy(alpha = 0.8f)) }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    userProfile?.aboutMe?.takeIf { it.isNotBlank() }?.let {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("О себе:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(it, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Фотографии:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showGallery() }, enabled = !isFetchingPhotos) {
                            Text("Посмотреть все фото (${allUserPhotos.size})")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    userProfile?.hobbies?.takeIf { it.isNotEmpty() }?.let { hobbies ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Увлечения:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            hobbies.forEach { hobbyName ->
                                val emoji = hobbyEmojiMap[hobbyName] ?: "❤️"
                                Text("$emoji $hobbyName", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
