package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class LoadMessages(val chatId: String) : ChatEvent()
}