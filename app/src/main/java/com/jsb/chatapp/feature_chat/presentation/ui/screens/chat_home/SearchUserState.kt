package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

import com.jsb.chatapp.feature_auth.domain.model.User

// SearchUserState.kt
data class SearchUserState(
    val query: String = "",
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

