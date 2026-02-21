package com.meowcat.app

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val navigateToFeed: () -> Unit = {
        navController.navigate(Screen.MatchFeed.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    val navigateToHobby: () -> Unit = {
        navController.navigate(Screen.Hobby.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    val checkUserInFirestore: (String) -> Unit = { userId ->
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.contains("hobbies")) navigateToFeed() else navigateToHobby()
            }
            .addOnFailureListener { navigateToHobby() } 
    }

    // --- ИСПРАВЛЕНО: Современный, надежный "One Tap" Sign-In ---
    val oneTapClient = remember { Identity.getSignInClient(context) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val googleIdToken = credential.googleIdToken
                if (googleIdToken != null) {
                    val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(googleCredential)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Успешно через Google!", Toast.LENGTH_LONG).show()
                            checkUserInFirestore(auth.currentUser?.uid.orEmpty())
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Ошибка Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Ошибка Google API: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Вход отменён", Toast.LENGTH_SHORT).show()
        }
    }

    val startGoogleSignIn: () -> Unit = {
        scope.launch {
            val signInRequest = GetSignInIntentRequest.builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()
            oneTapClient.getSignInIntent(signInRequest)
                .addOnSuccessListener { pendingIntent ->
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    googleLauncher.launch(intentSenderRequest)
                }
                .addOnFailureListener { e ->
                     Toast.makeText(context, "Ошибка подготовки входа: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    val registerUser: () -> Unit = {
        when {
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()

            password != confirmPassword ->
                Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()

            else -> {
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { authResult ->
                        Toast.makeText(context, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                        val uid = authResult.user?.uid ?: return@addOnSuccessListener
                        val initialData = mapOf("email" to email.trim(), "uid" to uid)
                        db.collection("users").document(uid).set(initialData)
                            .addOnSuccessListener { navigateToHobby() }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    Surface(color = Color(0xFFFFCC80), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Регистрация",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Почта)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.9f)
                )
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.9f)
                )
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Подтвердите пароль") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.9f)
                )
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = registerUser,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Зарегистрироваться", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
            Text("Или", color = Color.White.copy(0.8f))
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = startGoogleSignIn, // <-- ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(25.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Войти через Google", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(0.5f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("<< Назад", color = Color.White)
            }
        }
    }
}