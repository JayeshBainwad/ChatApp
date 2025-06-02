package com.jsb.chatapp.feature_chat.domain.model

import java.util.UUID

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val receiverId: String = "", // or groupId later
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)
