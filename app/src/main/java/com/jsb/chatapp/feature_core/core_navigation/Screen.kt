package com.jsb.chatapp.feature_core.core_navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Signin : Screen("signin")
    object Signup : Screen("signup")
    object ChatHome : Screen("chathome")
    object Profile : Screen("profile")
    object Chat : Screen("chat")
    object News : Screen("news")
    object Main : Screen("main")
}
