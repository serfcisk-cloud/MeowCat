package com.meowcat.app

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileSetupScreen(navController: NavController, hobbies: List<String>) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore

    remember {
        val config = mapOf(
            "cloud_name" to "dcyodbsms",
            "api_key" to "867571539639346",
            "api_secret" to "9zcWAM_he_lbiAs4U-wlQYgiTt8"
        )
        try {
            MediaManager.init(context, config)
        } catch (e: IllegalStateException) {
            Log.d("ProfileSetupScreen", "MediaManager already initialized")
        }
    }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(context, "Доступ к галерее необходим для выбора фото", Toast.LENGTH_LONG).show()
            }
        }
    )

    val finishProfileSetup: (String?) -> Unit = { imageUrl ->
        val user = auth.currentUser
        if (user != null) {
            val userProfile = hashMapOf(
                "email" to user.email,
                "hobbies" to hobbies,
                "profileImageUrl" to (imageUrl ?: "")
            )
            db.collection("users").document(user.uid).set(userProfile)
                .addOnSuccessListener {
                    isLoading = false
                    Toast.makeText(context, "Профиль сохранен!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Home.route) { popUpTo(0) }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Ошибка сохранения профиля: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } 
    }

    val uploadImageAndFinish: () -> Unit = {
        if (imageUri != null) {
            isLoading = true
            MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        finishProfileSetup(secureUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        isLoading = false
                        Toast.makeText(context, "Ошибка загрузки: ${error.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        } else {
            finishProfileSetup(null)
        }
    }

    Surface(color = Color(0xFF6A1B9A), modifier = Modifier.fillMaxSize()) { 
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Почти готово!",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Загрузи свое лучшее фото, чтобы другие котики могли тебя увидеть.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Выбранное фото",
                    modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.Gray).border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.cat_logo), 
                    contentDescription = "Плейсхолдер фото",
                    modifier = Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)).padding(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC80), contentColor = Color.Black)
            ) {
                Text("📸 Выбрать фото из галереи", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = uploadImageAndFinish,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                enabled = !isLoading 
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(if (imageUri != null) "Завершить" else "Пропустить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}