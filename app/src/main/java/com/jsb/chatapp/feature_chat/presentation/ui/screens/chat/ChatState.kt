package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import com.jsb.chatapp.feature_chat.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
