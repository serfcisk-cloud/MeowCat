package com.meowcat.app

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHobbyScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val user = auth.currentUser

    var selectedHobbies by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    // Загружаем текущие хобби пользователя
    LaunchedEffect(key1 = user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val hobbiesList = document.get("hobbies") as? List<String> ?: emptyList()
                        selectedHobbies = hobbiesList.toSet()
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    val saveHobbies: () -> Unit = {
        if (user != null) {
            isLoading = true
            db.collection("users").document(user.uid)
                .update("hobbies", selectedHobbies.toList())
                .addOnSuccessListener {
                    isLoading = false
                    Toast.makeText(context, "Увлечения сохранены!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack() // Возвращаемся назад
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Редактировать увлечения") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            })
        }
    ) {
        padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allHobbies) { hobby ->
                        val isSelected = selectedHobbies.contains(hobby.name)
                        Button(
                            onClick = {
                                if (isSelected) {
                                    selectedHobbies = selectedHobbies - hobby.name
                                } else if (selectedHobbies.size < 5) {
                                    selectedHobbies = selectedHobbies + hobby.name
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFE65100) else Color.White,
                                contentColor = if (isSelected) Color.White else Color(0xFFFF6600)
                            )
                        ) {
                            Text(text = "${hobby.emoji} ${hobby.name}", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = saveHobbies,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}