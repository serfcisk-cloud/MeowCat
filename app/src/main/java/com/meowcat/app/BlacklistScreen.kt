package com.meowcat.app

import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class BlockedUser(val uid: String, val nickname: String?, val profileImageUrl: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(navController: NavController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val myId = currentUser?.uid ?: ""

    var blockedUsers by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchBlockedUsers() {
        if (myId.isBlank()) return
        isLoading = true
        db.collection("users").document(myId).get().addOnSuccessListener { myDoc ->
            val blockedIds = myDoc.get("blockedUsers") as? List<String> ?: emptyList()
            if (blockedIds.isEmpty()) {
                isLoading = false
                blockedUsers = emptyList()
                return@addOnSuccessListener
            }
            db.collection("users").whereIn("uid", blockedIds).get().addOnSuccessListener { usersSnapshot ->
                val users = usersSnapshot.map {
                    BlockedUser(
                        uid = it.id,
                        nickname = it.getString("nickname"),
                        profileImageUrl = it.getString("profileImageUrl")
                    )
                }
                blockedUsers = users
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(myId) {
        fetchBlockedUsers()
    }

    fun unblockUser(userIdToUnblock: String) {
        db.collection("users").document(myId)
            .update("blockedUsers", FieldValue.arrayRemove(userIdToUnblock))
            .addOnSuccessListener {
                Toast.makeText(context, "Пользователь разблокирован", Toast.LENGTH_SHORT).show()
                fetchBlockedUsers() // Обновляем список
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Черный список") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Назад") } }) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (blockedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Черный список пуст") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(blockedUsers) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = user.profileImageUrl ?: R.drawable.cat_logo, contentDescription = "Аватар", modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(user.nickname ?: "Аноним", fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { unblockUser(user.uid) }) {
                            Text("Разблокировать")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}