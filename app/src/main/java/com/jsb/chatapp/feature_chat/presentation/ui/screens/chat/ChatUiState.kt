package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import com.jsb.chatapp.feature_chat.domain.model.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val messageInput: String = ""
)
