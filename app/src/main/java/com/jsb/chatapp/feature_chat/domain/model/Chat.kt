package com.jsb.chatapp.feature_chat.domain.model

import com.jsb.chatapp.feature_main.main_domain.main_model.User

data class Chat(
    val chatId: String,
    val lastMessage: String,
    val timestamp: Long,
    val participants: List<String>,
    val otherUser: User,
    val unreadCount: Int = 0 // Add unread count to the model
)