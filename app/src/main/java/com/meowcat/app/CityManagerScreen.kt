package com.meowcat.app

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun CityManagerScreen(navController: NavController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser

    var isLoading by remember { mutableStateOf(true) }
    var myCountry by remember { mutableStateOf("") }
    var myCity by remember { mutableStateOf("") }
    var seekingCountry by remember { mutableStateOf("") }
    var seekingCity by remember { mutableStateOf("") }
    var myCountryExpanded by remember { mutableStateOf(false) }
    var seekingCountryExpanded by remember { mutableStateOf(false) }

    val countries = listOf(
        "Россия", "Україна", "Беларусь", "Қазақстан", "Polska", "Deutschland",
        "United Kingdom", "France", "España", "Italia", "Nederland", "Sverige",
        "中国", "日本", "대한민국", "भारत", "Türkiye", "ประเทศไทย", "Việt Nam",
        "United States", "Canada", "Brasil", "México", "Argentina"
    )

    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        myCountry = doc.getString("my_country") ?: ""
                        myCity = doc.getString("my_city") ?: ""
                        seekingCountry = doc.getString("seeking_country") ?: ""
                        seekingCity = doc.getString("seeking_city") ?: ""
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
        } else {
            isLoading = false
        }
    }

    fun saveLocations() {
        if (user != null) {
            val dataToUpdate = mapOf(
                "my_country" to myCountry,
                "my_city" to myCity,
                "seeking_country" to seekingCountry,
                "seeking_city" to seekingCity
            )
            db.collection("users").document(user.uid)
                .update(dataToUpdate)
                .addOnSuccessListener {
                    Toast.makeText(context, "Настройки местоположения сохранены!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Surface(color = Color(0xFF6A1B9A), modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Управление местоположением", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Мое местоположение", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(expanded = myCountryExpanded, onExpandedChange = { myCountryExpanded = !myCountryExpanded }) {
                         TextField(
                            value = myCountry,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Выберите вашу страну") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = myCountryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha=0.9f), unfocusedContainerColor = Color.White.copy(alpha=0.9f))
                        )
                        ExposedDropdownMenu(
                            expanded = myCountryExpanded,
                            onDismissRequest = { myCountryExpanded = false }
                        ) {
                            countries.sorted().forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country) },
                                    onClick = {
                                        myCountry = country
                                        myCountryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = myCity,
                        onValueChange = { myCity = it },
                        label = { Text("Введите ваш город") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Искать в", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = seekingCountryExpanded, onExpandedChange = { seekingCountryExpanded = !seekingCountryExpanded }) {
                        TextField(
                            value = seekingCountry,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Выберите страну для поиска") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seekingCountryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha=0.9f), unfocusedContainerColor = Color.White.copy(alpha=0.9f))
                        )
                        ExposedDropdownMenu(
                            expanded = seekingCountryExpanded,
                            onDismissRequest = { seekingCountryExpanded = false }
                        ) {
                            countries.sorted().forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country) },
                                    onClick = {
                                        seekingCountry = country
                                        seekingCountryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = seekingCity,
                        onValueChange = { seekingCity = it },
                        label = { Text("Введите город (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = ::saveLocations,
                        enabled = myCountry.isNotBlank() && seekingCountry.isNotBlank() && myCity.isNotBlank()
                    ) {
                        Text("Сохранить и вернуться")
                    }
                }
            }
        }
    }
}