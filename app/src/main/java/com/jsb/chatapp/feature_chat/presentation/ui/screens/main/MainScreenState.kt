package com.jsb.chatapp.feature_chat.presentation.ui.screens.main

import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.domain.model.Chat

data class MainScreenState(
    val currentUser: User? = null,
    val availableChats: List<Chat> = emptyList(),
    val selectedOtherUser: User? = null,
    val selectedChatId: String? = null,
    val selectedUserIdForChat: String? = null, // Add this field to track which user is selected
    val currentRoute: String? = null,
    val showSignOutDialog: Boolean = false
)