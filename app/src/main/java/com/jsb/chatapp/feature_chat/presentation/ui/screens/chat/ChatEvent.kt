package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

sealed class ChatEvent {
    data class OnMessageInputChanged(val input: String) : ChatEvent()
    object OnSendMessage : ChatEvent()
    object OnMarkMessagesSeen : ChatEvent()
}