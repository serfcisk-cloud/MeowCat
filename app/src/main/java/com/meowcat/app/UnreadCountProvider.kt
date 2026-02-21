package com.meowcat.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun rememberTotalUnreadCount(): State<Int> {
    val currentUserId = Firebase.auth.currentUser?.uid ?: return produceState(initialValue = 0) { }

    return produceState(initialValue = 0, currentUserId) {
        val db = Firebase.firestore
        val listener = db.collection("chats_users")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    value = 0
                    return@addSnapshotListener
                }

                var totalUnread = 0
                for (doc in snapshot.documents) {
                    val count = doc.getLong("unreadCount_$currentUserId") ?: 0L
                    totalUnread += count.toInt()
                }
                value = totalUnread
            }

        // Этот блок будет вызван, когда Composable покинет экран
        awaitDispose {
            listener.remove()
        }
    }
}