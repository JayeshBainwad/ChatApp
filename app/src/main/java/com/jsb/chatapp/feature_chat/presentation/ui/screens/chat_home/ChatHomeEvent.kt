package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

// SearchUserEvent.kt
sealed class ChatHomeEvent {
    data class OnQueryChange(val query: String) : ChatHomeEvent()
}
