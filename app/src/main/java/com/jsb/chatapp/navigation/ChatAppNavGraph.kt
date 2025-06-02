package com.jsb.chatapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen

@Composable
fun ChatAppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Signin.route) {
            SignInScreen(navController)
        }
        composable(Screen.Signup.route) {
            SignupScreen(navController)
        }
        composable(Screen.Chat.route) { backStackEntry ->
            ChatScreen(
                chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                navController = navController
            )
        }
    }
}