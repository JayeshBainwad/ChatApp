package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

import com.jsb.chatapp.feature_auth.domain.model.User

data class ChatHomeState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),       // Either searched users or chatted users
    val error: String? = null,
    val query: String = ""                     // Search query
)
