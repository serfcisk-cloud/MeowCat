package com.meowcat.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val user = auth.currentUser

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var nickname by remember { mutableStateOf("") }
    var aboutMe by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var hobbies by remember { mutableStateOf<List<String>>(emptyList()) }
    var gender by remember { mutableStateOf<String?>(null) }
    var seeking by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf(18f) }
    var ageRange by remember { mutableStateOf(18f..35f) }
    var height by remember { mutableStateOf(170f) }
    var heightRange by remember { mutableStateOf(150f..190f) }

    val hobbyEmojiMap = remember { allHobbies.associateBy({ it.name }, { it.emoji }) }

    LaunchedEffect(key1 = user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        nickname = document.getString("nickname") ?: ""
                        aboutMe = document.getString("about_me") ?: ""
                        profileImageUrl = document.getString("profileImageUrl")
                        hobbies = document.get("hobbies") as? List<String> ?: emptyList()
                        gender = document.getString("gender")
                        seeking = document.getString("seeking")
                        age = (document.getDouble("age")?.toFloat()) ?: 18f
                        ageRange = (document.getDouble("seeking_age_min")?.toFloat() ?: 18f)..(document.getDouble("seeking_age_max")?.toFloat() ?: 35f)
                        height = (document.getDouble("height")?.toFloat()) ?: 170f
                        heightRange = (document.getDouble("seeking_height_min")?.toFloat() ?: 150f)..(document.getDouble("seeking_height_max")?.toFloat() ?: 190f)
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    val saveProfileAndContinue: () -> Unit = {
        if (user != null) {
            isSaving = true
            val updatedProfile = mapOf(
                "nickname" to nickname,
                "about_me" to aboutMe,
                "gender" to gender,
                "seeking" to seeking,
                "age" to age,
                "seeking_age_min" to ageRange.start,
                "seeking_age_max" to ageRange.endInclusive,
                "height" to height,
                "seeking_height_min" to heightRange.start,
                "seeking_height_max" to heightRange.endInclusive
            )
            db.collection("users").document(user.uid).update(updatedProfile)
                .addOnSuccessListener {
                    isSaving = false
                    navController.navigate(Screen.MatchFeed.route)
                }
                .addOnFailureListener { isSaving = false }
        }
    }

    // --- ИСПРАВЛЕНО: Добавлен фиолетовый фон --- 
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF6A1B9A)
    ) {
        Scaffold(
            containerColor = Color.Transparent, // <--- Делаем Scaffold прозрачным
            bottomBar = {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .fillMaxWidth()
                        .background(Color.Transparent) // <--- Делаем фон нижнего бара прозрачным
                ) {
                    Button(
                        onClick = saveProfileAndContinue,
                        enabled = !isSaving && nickname.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isSaving) "Сохранение..." else "Сохранить и продолжить",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = profileImageUrl ?: R.drawable.cat_logo,
                            contentDescription = "Аватар пользователя",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { navController.navigate(Screen.PhotoManager.route) },
                            modifier = Modifier.background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Добавить фото", tint = Color(0xFF6A1B9A))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Ваш псевдоним") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedLabelColor = Color(0xFFFFCC80)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = aboutMe,
                        onValueChange = { if (it.length <= 100) aboutMe = it },
                        label = { Text("О себе (не более 100 символов)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedLabelColor = Color(0xFFFFCC80)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Я...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ToggleButton(text = "Мужчина", isSelected = gender == "Мужчина") { gender = "Мужчина" }
                        Spacer(modifier = Modifier.width(8.dp))
                        ToggleButton(text = "Женщина", isSelected = gender == "Женщина") { gender = "Женщина" }
                        Spacer(modifier = Modifier.width(8.dp))
                        ToggleButton(text = "Другое", isSelected = gender == "Другое") { gender = "Другое" }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Ищу...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ToggleButton(text = "Мужчину", isSelected = seeking == "Мужчина") { seeking = "Мужчина" }
                        Spacer(modifier = Modifier.width(8.dp))
                        ToggleButton(text = "Женщину", isSelected = seeking == "Женщина") { seeking = "Женщина" }
                        Spacer(modifier = Modifier.width(8.dp))
                        ToggleButton(text = "Другое", isSelected = seeking == "Другое") { seeking = "Другое" }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Мой возраст: ${age.roundToInt()}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Slider(value = age, onValueChange = { age = it }, valueRange = 18f..99f, steps = 80)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Ищу партнера в возрасте от ${ageRange.start.roundToInt()} до ${ageRange.endInclusive.roundToInt()} лет", color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    RangeSlider(value = ageRange, onValueChange = { ageRange = it }, valueRange = 18f..99f, steps = 80)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Мой рост: ${height.roundToInt()} см", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Slider(value = height, onValueChange = { height = it }, valueRange = 120f..220f, steps = 99)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Ищу партнера ростом от ${heightRange.start.roundToInt()} до ${heightRange.endInclusive.roundToInt()} см", color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    RangeSlider(value = heightRange, onValueChange = { heightRange = it }, valueRange = 120f..220f, steps = 99)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (hobbies.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Мои увлечения:", color = Color.White.copy(alpha = 0.8f))
                            IconButton(onClick = { navController.navigate(Screen.EditHobby.route) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать хобби", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            hobbies.forEachIndexed { index, hobby ->
                                val emoji = hobbyEmojiMap[hobby] ?: "❤️"
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$emoji $hobby", color = Color.White)
                                }
                                if (index < hobbies.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // Пустое место для прокрутки — кнопка теперь в bottomBar!
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ToggleButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.2f),
            contentColor = if (isSelected) Color.Black else Color.White
        )
    ) {
        Text(text)
    }
}
