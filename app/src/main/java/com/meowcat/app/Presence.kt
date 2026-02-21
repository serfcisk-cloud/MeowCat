package com.meowcat.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun rememberUserPresence(userId: String): State<Boolean> {
    val isOnline = remember { mutableStateOf(false) }

    DisposableEffect(userId) {
        if (userId.isEmpty()) {
            onDispose {}
        } else {
            val db = Firebase.firestore
            val userStatusRef = db.collection("users").document(userId)

            val listener = userStatusRef.addSnapshotListener { snapshot, _ ->
                // Мы предполагаем, что в документе пользователя есть поле 'online' типа Boolean
                val onlineStatus = snapshot?.getBoolean("online") ?: false
                isOnline.value = onlineStatus
            }

            onDispose {
                listener.remove()
            }
        }
    }

    return isOnline
}
