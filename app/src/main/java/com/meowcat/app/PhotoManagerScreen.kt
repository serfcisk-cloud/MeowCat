package com.meowcat.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoManagerScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = Firebase.firestore
    val user = auth.currentUser
    val scope = rememberCoroutineScope()

    var photoUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var mainPhotoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val setAsMainPhoto: (String) -> Unit = { url ->
        if (user != null) {
            mainPhotoUrl = url
            db.collection("users").document(user.uid).update("profileImageUrl", url)
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val gallery = document.get("photo_gallery") as? List<String> ?: emptyList()
                        val mainImage = document.getString("profileImageUrl")
                        mainPhotoUrl = mainImage
                        val allImages = (gallery + listOfNotNull(mainImage)).distinct()
                        photoUrls = allImages
                        if (allImages.size > gallery.size) {
                            db.collection("users").document(user.uid).update("photo_gallery", allImages)
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { 
                    Toast.makeText(context, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        } else {
             isLoading = false
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            val currentUser = auth.currentUser
            if (uri != null && currentUser != null) {
                isLoading = true
                
                try {
                    // ВАЖНО: Здесь должен быть ваш Unsigned Upload Preset из панели Cloudinary
                    // Например: "meowcat_upload" (создайте его в настройках Cloudinary без подписи)
                    MediaManager.get().upload(uri)
                        .option("upload_preset", "ВАШ_UNSIGNED_PRESET") 
                        .callback(object : UploadCallback {
                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                val newUrl = resultData["secure_url"] as? String
                                if (newUrl != null && scope.isActive) {
                                    // КРИТИЧЕСКИ ВАЖНО: Обновляем UI только в главном потоке!
                                    scope.launch(Dispatchers.Main) {
                                        val isFirstPhoto = mainPhotoUrl.isNullOrEmpty()
                                        photoUrls = photoUrls + newUrl
                                        
                                        db.collection("users").document(currentUser.uid)
                                            .update("photo_gallery", FieldValue.arrayUnion(newUrl))
                                            .addOnSuccessListener {
                                                if (isFirstPhoto) {
                                                    setAsMainPhoto(newUrl)
                                                }
                                                isLoading = false
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Ошибка БД: ${e.message}", Toast.LENGTH_LONG).show()
                                                photoUrls = photoUrls.filter { it != newUrl }
                                                isLoading = false
                                            }
                                    }
                                }
                            }

                            override fun onError(requestId: String, error: ErrorInfo) {
                                if (scope.isActive) {
                                    scope.launch(Dispatchers.Main) {
                                        Toast.makeText(context, "Ошибка Cloudinary: ${error.description}", Toast.LENGTH_LONG).show()
                                        isLoading = false
                                    }
                                }
                            }

                            override fun onStart(requestId: String) {}
                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                            override fun onReschedule(requestId: String, error: ErrorInfo) {}
                        }).dispatch()
                        
                } catch (e: Exception) {
                    // Если Cloudinary не инициализирован, приложение НЕ вылетит, а покажет ошибку
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка инициализации загрузки. Проверьте настройки приложения.", Toast.LENGTH_LONG).show()
                        isLoading = false
                    }
                }
            } else if (uri == null) {
                Toast.makeText(context, "Выбор отменен", Toast.LENGTH_SHORT).show()
            }
        }
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Доступ к галерее необходим для добавления фото!", Toast.LENGTH_LONG).show()
        }
    }
    
    val addPhotoClick: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои фотографии") }, 
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
                // --- КНОПКА ВЫХОДА УДАЛЕНА ---
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(onClick = addPhotoClick) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить фото")
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            photoUrls.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("У вас пока нет фотографий", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = addPhotoClick) {
                        Text("Добавить первое фото")
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photoUrls) { url ->
                        val isMain = url == mainPhotoUrl
                        Box(contentAlignment = Alignment.TopEnd) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Фото пользователя",
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxSize()
                                    .border(BorderStroke(if (isMain) 4.dp else 0.dp, Color(0xFF6A1B9A)))
                                    .clickable { setAsMainPhoto(url) },
                                contentScale = ContentScale.Crop
                            )
                            if(isMain) {
                                Text("🔥", modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha=0.5f), CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}