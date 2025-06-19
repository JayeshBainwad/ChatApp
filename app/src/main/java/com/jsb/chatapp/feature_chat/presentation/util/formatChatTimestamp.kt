package com.jsb.chatapp.util

import java.text.SimpleDateFormat
import java.util.*

fun formatChatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""

    val messageTime = Date(timestamp)
    val currentTime = Date()
    val diff = currentTime.time - messageTime.time

    val oneDayMillis = 24 * 60 * 60 * 1000

    val format = if (diff < oneDayMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()) // e.g., 14:35
    } else {
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()) // e.g., 12/06/24
    }

    return format.format(messageTime)
}
