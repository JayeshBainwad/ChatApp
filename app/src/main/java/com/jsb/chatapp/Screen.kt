package com.jsb.chatapp

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Signin : Screen("signin")
    object Signup : Screen("signup")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
}