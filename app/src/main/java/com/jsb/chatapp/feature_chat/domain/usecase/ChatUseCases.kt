package com.jsb.chatapp.feature_chat.domain.usecase

data class ChatUseCases(
    val getChatsForUser: GetChatsForUserUseCase,
    val listenForMessages: ListenForMessagesUseCase
)
