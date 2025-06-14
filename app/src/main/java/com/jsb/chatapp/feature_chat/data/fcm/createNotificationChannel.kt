package com.jsb.chatapp.feature_chat.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "chat_channel",
            "Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
