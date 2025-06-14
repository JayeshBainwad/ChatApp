package com.jsb.chatapp.feature_chat.data.fcm

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jsb.chatapp.feature_chat.domain.usecase.UpdateFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
        Log.d("FCM", "Message data payload: ${remoteMessage.data}")
        remoteMessage.notification?.let {
            Log.d("FCM", "Notification Title: ${it.title}")
            Log.d("FCM", "Notification Body: ${it.body}")
        }
        val title = remoteMessage.notification?.title ?: "New Message"
        val message = remoteMessage.notification?.body ?: return
        NotificationHelper.showNotification(this, title, message)
    }
}
