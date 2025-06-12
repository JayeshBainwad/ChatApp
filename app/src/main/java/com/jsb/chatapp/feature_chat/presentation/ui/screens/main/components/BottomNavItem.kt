package com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components

sealed class BottomNavItem(val route: String, val label: String) {
    object ChatHome : BottomNavItem("chathome", "Chats")
    object Profile : BottomNavItem("profile", "Profile")
    object News : BottomNavItem("news", "News")
}
