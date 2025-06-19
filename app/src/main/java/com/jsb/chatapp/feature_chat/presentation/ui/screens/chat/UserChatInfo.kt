package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import com.jsb.chatapp.feature_core.core_domain.main_model.User

data class UserChatInfo(
    val user: User,
    val lastMessage: String? = null,
    val timestamp: Long? = null,
    val hasExistingChat: Boolean = false,
    val unreadCount: Int = 0 // Add unread count
)