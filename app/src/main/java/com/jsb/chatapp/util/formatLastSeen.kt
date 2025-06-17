package com.jsb.chatapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatLastSeen(lastSeenTimestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - lastSeenTimestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(lastSeenTimestamp))
        }
    }
}

fun getOnlineStatusText(isOnline: Boolean, lastSeen: Long): String {
    return if (isOnline) {
        "Online"
    } else {
        "Last seen ${formatLastSeen(lastSeen)}"
    }
}