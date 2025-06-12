package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import com.jsb.chatapp.feature_auth.domain.model.User

data class UserChatInfo(
    val user: User,
    val lastMessage: String? = null,
    val timestamp: Long? = null,
    val hasExistingChat: Boolean = false
)
