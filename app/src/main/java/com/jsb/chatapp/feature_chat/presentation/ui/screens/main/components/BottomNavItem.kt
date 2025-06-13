package com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components

import androidx.annotation.DrawableRes
import com.jsb.chatapp.R

sealed class BottomNavItem(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int
) {
    object ChatHome : BottomNavItem(
        route = "chathome",
        label = "Chats",
        icon = R.drawable.chat
    )

    object Profile : BottomNavItem(
        route = "profile",
        label = "Profile",
        icon = R.drawable.profile
    )

    object News : BottomNavItem(
        route = "news",
        label = "News",
        icon = R.drawable.news
    )
}