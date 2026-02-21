package com.meowcat.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Auth : Screen("auth")
    object Registration : Screen("registration")
    object Hobby : Screen("hobby")
    object EditHobby : Screen("edit_hobby")
    object ProfileSetup : Screen("profile_setup") 
    object Home : Screen("home") 
    object PhotoManager : Screen("photo_manager")
    object CityManager : Screen("city_manager")
    object UserProfile: Screen("user_profile")
    object Chat: Screen("chat")
    object Blacklist: Screen("blacklist")
    object Friends: Screen("friends")
    object ChatList: Screen("chat_list")
    object Notifications: Screen("notifications")
    object License: Screen("license") 
    object MatchFeed : Screen("match_feed")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val auth = Firebase.auth

    // --- НОВАЯ ЛОГИКА: Управление онлайн-статусом ---
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = Firebase.firestore

    DisposableEffect(lifecycleOwner, auth.currentUser) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return@DisposableEffect onDispose {}
        }

        val userStatusRef = db.collection("users").document(userId)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    userStatusRef.update("online", true)
                }
                Lifecycle.Event.ON_STOP -> {
                    userStatusRef.update("online", false)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val licenseAccepted = prefs.getBoolean("license_accepted", false)

    val startDestination = if (!licenseAccepted) {
        Screen.License.route
    } else if (auth.currentUser != null) {
        Screen.MatchFeed.route
    } else {
        Screen.Main.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.License.route) { LicenseScreen(navController = navController) }
        composable(Screen.Main.route) { MainCatScreen(navController = navController) }
        composable(Screen.Auth.route) { AuthScreen(navController = navController) }
        composable(Screen.Registration.route) { RegistrationScreen(navController = navController) }
        composable(Screen.Hobby.route) { HobbyScreen(navController = navController) }
        composable(Screen.EditHobby.route) { EditHobbyScreen(navController = navController) }
        composable(
            route = "${Screen.ProfileSetup.route}/{hobbies}",
            arguments = listOf(navArgument("hobbies") { type = NavType.StringType })
        ) { backStackEntry ->
            val hobbies = backStackEntry.arguments?.getString("hobbies")?.split(",") ?: emptyList()
            ProfileSetupScreen(navController = navController, hobbies = hobbies)
        }
        composable(Screen.Home.route) { HomeScreen(navController = navController) }
        composable(Screen.PhotoManager.route) { PhotoManagerScreen(navController = navController) }
        composable(Screen.CityManager.route) { CityManagerScreen(navController = navController) }
        composable(
            route = "${Screen.UserProfile.route}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(navController = navController, userId = userId)
        }
        composable(
            route = "${Screen.Chat.route}/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(navController = navController, chatId = chatId)
        }
        composable(Screen.Blacklist.route) {
            BlacklistScreen(navController = navController)
        }
        composable(Screen.Friends.route) {
            FriendsScreen(navController = navController)
        }
        composable(Screen.ChatList.route) {
            ChatListScreen(navController = navController)
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(navController = navController)
        }
        composable(Screen.MatchFeed.route) { MatchFeedScreen(navController = navController) }
    }
}