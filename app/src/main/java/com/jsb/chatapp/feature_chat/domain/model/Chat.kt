package com.jsb.chatapp.feature_chat.domain.model

import com.jsb.chatapp.feature_auth.domain.model.User

data class Chat(
    val chatId: String,
    val lastMessage: String,
    val timestamp: Long,
    val participants: List<String>,
    val otherUser: User
)