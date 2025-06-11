package com.jsb.chatapp

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Signin : Screen("signin")
    object Signup : Screen("signup")
    object ChatHome : Screen("chathome")
    object Profile : Screen("profile")
    object Chat : Screen("chat") // simple route, no parameters
}
