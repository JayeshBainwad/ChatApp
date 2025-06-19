package com.jsb.chatapp.feature_chat.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jsb.chatapp.feature_main.main_navigation.MainActivity
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_chat.domain.usecase.UpdateFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var updateFcmTokenUseCase: UpdateFcmTokenUseCase

    override fun onNewToken(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("FCM", "New FCM token generated: $token")

        CoroutineScope(Dispatchers.IO).launch {
            updateFcmTokenUseCase(userId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM_RECEIVED", "Message received from: ${remoteMessage.from}")
        Log.d("FCM_RECEIVED", "Message data: ${remoteMessage.data}")

        val senderName = remoteMessage.data["senderName"]
        val content = remoteMessage.data["content"]
        val senderId = remoteMessage.data["senderId"]
        val receiverId = remoteMessage.data["receiverId"] // not used
        val senderFcmToken = remoteMessage.data["fcmToken"]

        if (senderName != null && content != null && senderId != null && senderFcmToken != null
            && receiverId != null ) {
            showNotification(senderName, content, senderId, receiverId, senderFcmToken)
        } else {
            Log.w("FCM_RECEIVED", "Missing fields in notification data")
        }
    }

    private fun showNotification(
        senderName: String,
        content: String,
        senderId: String,
        receiverId: String,
        senderFcmToken: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateToChat", true)
            putExtra("senderId", senderId)
            putExtra("receiverId", receiverId)
            putExtra("senderName", senderName)
            putExtra("senderFcmToken", senderFcmToken)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(senderName)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

}
