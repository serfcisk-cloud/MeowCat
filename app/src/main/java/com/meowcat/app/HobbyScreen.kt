package com.meowcat.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HobbyScreen(navController: NavController) {
    var selectedHobbies by remember { mutableStateOf(setOf<Hobby>()) }

    Scaffold(
        // Кнопка всегда над навигационной панелью и клавиатурой
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()   // ← под 3 кнопки или жесты
                    .imePadding()              // ← если вдруг клавиатура
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Button(
                    onClick = {
                        val hobbiesString = selectedHobbies.joinToString(",") { it.name }
                        navController.navigate("${Screen.ProfileSetup.route}/$hobbiesString")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = selectedHobbies.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "Продолжить",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Чем ты увлекаешься?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            Text(
                text = "Выбери до 5 увлечений",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allHobbies) { hobby ->
                    val isSelected = selectedHobbies.contains(hobby)
                    Button(
                        onClick = {
                            if (isSelected) {
                                selectedHobbies = selectedHobbies - hobby
                            } else if (selectedHobbies.size < 5) {
                                selectedHobbies = selectedHobbies + hobby
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFE65100) else Color.White,
                            contentColor = if (isSelected) Color.White else Color(0xFFFF6600)
                        ),
                        modifier = Modifier.aspectRatio(1.5f)
                    ) {
                        Text(text = "${hobby.emoji} ${hobby.name}", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}