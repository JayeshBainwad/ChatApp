package com.jsb.chatapp.feature_chat.domain.model

import java.util.UUID

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val senderName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus { SENDING, SENT, RECEIVED, DELIVERED, SEEN }