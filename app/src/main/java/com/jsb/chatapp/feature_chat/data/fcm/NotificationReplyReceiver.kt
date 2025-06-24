package com.jsb.chatapp.feature_chat.data.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.NotificationChannel
import android.app.PendingIntent
import android.os.Bundle
import com.jsb.chatapp.feature_core.core_navigation.MainActivity

@AndroidEntryPoint
class NotificationReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository
    // Add these companion object fields to track message status
    companion object {
        // Track sent messages to check their status later
        private val sentMessageIds = mutableSetOf<String>()

        // Track messages that have been sent for delayed notification
        private val pendingNotificationMessages = mutableMapOf<String, Message>()

        fun clearConversationAndNotification(context: Context, senderId: String) {
            // Clear from MyFirebaseService conversation map
            MyFirebaseService.clearConversationStatic(senderId)

            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(senderId.hashCode())
        }
        /**
         * Generate consistent chat ID for database operations
         * Must match the format used in ChatViewModel/Repository
         */
        fun generateChatId(user1: String, user2: String): String {
            return listOf(user1, user2).sorted().joinToString("_")
        }
    }
    /**
     * Handle reply action from notification
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MyFirebaseService.REPLY_ACTION) {
            Log.d("FCM_REPLY", "Reply action received")

            // Extract reply text from RemoteInput - DO NOT clear it immediately
            val bundle = RemoteInput.getResultsFromIntent(intent)
            val replyText = bundle?.getCharSequence(MyFirebaseService.KEY_TEXT_REPLY)?.toString()

            // Extract user data from intent
            val senderId = intent.getStringExtra("senderId")
            val receiverId = intent.getStringExtra("receiverId")
            val senderName = intent.getStringExtra("senderName")
            val receiverName = intent.getStringExtra("receiverName")
            val senderFcmToken = intent.getStringExtra("senderFcmToken")
            val receiverFcmToken = intent.getStringExtra("receiverFcmToken")

            // Validate all required data is present
            if (!replyText.isNullOrBlank() && senderId != null && receiverId != null &&
                senderName != null && senderFcmToken != null && receiverFcmToken != null &&
                receiverName != null) {

                Log.d("FCM_REPLY", "Processing reply: $replyText from $senderName to $receiverName")

                // Add user reply to conversation BEFORE updating notification
                MyFirebaseService.addUserReplyToConversationStatic(receiverId, replyText, senderName)

                // Send the reply message to Firestore
                sendReplyMessage(
                    context = context,
                    replyText = replyText,
                    senderId = senderId,
                    receiverId = receiverId,
                    senderName = senderName,
                    receiverName = receiverName,
                    senderFcmToken = senderFcmToken,
                    receiverFcmToken = receiverFcmToken
                )

                // Update notification AFTER a short delay to avoid conflicts
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500) // Small delay to ensure message is processed
                    updateNotificationWithReply(
                        senderId = senderId,
                        context = context,
                        originalSenderId = receiverId,
                        currentUserId = senderId,
                        originalSenderName = receiverName,
                        currentUserName = senderName,
                        replyText = replyText,
                        senderFcmToken = senderFcmToken,
                        receiverFcmToken = receiverFcmToken
                    )
                }

            } else {
                Log.e("FCM_REPLY", "Missing or empty reply text or required data")
            }
        }
    }

    /**
     * Send reply message to Firestore database
     * The message will trigger FCM notification through your existing system
     */
    private fun sendReplyMessage(
        context: Context,
        replyText: String,
        senderId: String, // Current user
        receiverId: String, // Other user
        senderName: String, // Current user name
        receiverName: String, // Other user name
        senderFcmToken: String, // Current user FCM token
        receiverFcmToken: String // Other user FCM token
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("FCM_REPLY", "User not authenticated")
            showErrorNotification(context, "Authentication failed")
            return
        }

        // Verify current user ID matches senderId
        if (currentUser.uid != senderId) {
            Log.e("FCM_REPLY", "User ID mismatch. Expected: $senderId, Got: ${currentUser.uid}")
            showErrorNotification(context, "User authentication mismatch")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Generate chat ID (same format used throughout the app)
                val chatId = generateChatId(senderId, receiverId)
                Log.d("FCM_REPLY", "Generated chatId: $chatId for users: $senderId -> $receiverId")

                // Create message object
                val message = Message(
                    senderId = senderId, // Current user
                    receiverId = receiverId, // Other user
                    content = replyText,
                    senderName = senderName, // Current user name
                    status = MessageStatus.SENT,
                    timestamp = System.currentTimeMillis()
                )

                Log.d("FCM_REPLY", "Sending message to Firestore: ${message.content}")

                // Send message to Firestore using ChatRepository
                // This will trigger your existing FCM notification system
                chatRepository.sendMessage(chatId, message)

                Log.d("FCM_REPLY", "Message sent to Firestore successfully")

                // Start listening for message status changes for this chat
                listenForMessageStatusChanges(context, chatId)

                // Start monitoring this message for delayed notification
                startMessageStatusMonitoring(
                    context = context,
                    message = message,
                    chatId = chatId,
                    receiverFcmToken = receiverFcmToken,
                    senderFcmToken = senderFcmToken,
                    senderName = senderName,
                    receiverName = receiverName,
                    senderId = senderId,
                    receiverId = receiverId
                )

                Log.d("FCM_REPLY", "Notification sent successfully")

            } catch (e: Exception) {
                Log.e("FCM_REPLY", "Failed to send reply message", e)
                showErrorNotification(context, "Failed to send message: ${e.message}")
            }
        }
    }

    private fun listenForMessageStatusChanges(context: Context, chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            chatRepository.listenForMessages(chatId) { messages ->
                checkMessageStatusChanges(context, messages)
            }
        }
    }

    private fun checkMessageStatusChanges(context: Context, currentMessages: List<Message>) {
        val currentMessagesMap = currentMessages.associateBy { it.messageId }

        // Check all pending messages for status changes
        pendingNotificationMessages.keys.toList().forEach { messageId ->
            val currentMessage = currentMessagesMap[messageId]

            if (currentMessage != null && currentMessage.status == MessageStatus.SEEN) {
                pendingNotificationMessages.remove(messageId)
                sentMessageIds.remove(messageId)
                Log.d("MESSAGE_STATUS", "Reply message $messageId was seen - notification cancelled")

                // Clear conversation and notification when message is seen
                clearConversationAndNotification(context, currentMessage.senderId)
            }
        }
    }

    private fun startMessageStatusMonitoring(
        context: Context,
        message: Message,
        chatId: String,
        receiverFcmToken: String,
        senderFcmToken: String,
        senderName: String,
        receiverName: String,
        senderId: String,
        receiverId: String
    ) {
        // Add to pending notifications
        pendingNotificationMessages[message.messageId] = message
        sentMessageIds.add(message.messageId)

        CoroutineScope(Dispatchers.IO).launch {
            // Wait for 2 seconds
            delay(2000L)

            // Check if message is still pending (not seen)
            val pendingMessage = pendingNotificationMessages[message.messageId]
            if (pendingMessage != null && receiverFcmToken.isNotEmpty()) {

                Log.d("SMART_NOTIFICATION", "Reply message ${message.messageId} not seen after 2 seconds, sending notification")

                ChatViewModel.FCMSender.sendPushNotification(
                    serverUrl = "http://192.168.0.109:8080/send-notification",
                    otherUserFcmToken = receiverFcmToken,
                    currentUserFcmToken = senderFcmToken,
                    senderName = senderName,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = message.content,
                    receiverName = receiverName
                )

                // Remove from pending after sending notification
                pendingNotificationMessages.remove(message.messageId)
            }
        }
    }

    /**
     * Update the current notification to show the reply was sent
     * This gives user feedback that their reply was processed test1@gmail.com
     */
    private fun updateNotificationWithReply(
        senderId: String,
        context: Context,
        originalSenderId: String, // Original message sender (for notification ID)
        currentUserId: String, // Current user who replied
        originalSenderName: String, // Original message sender name
        currentUserName: String, // Current user name
        replyText: String,
        senderFcmToken: String,
        receiverFcmToken: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_notifications"

        val chatId = originalSenderId + currentUserId

        // Create intent for opening the chat
        val openChatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateToChat", true)
            putExtra("senderId", currentUserId) // Current user
            putExtra("receiverId", originalSenderId) // Other user
            putExtra("senderName", currentUserName) // Current user name
            putExtra("receiverName", originalSenderName) // Other user name
            putExtra("clearNotification", true)
            putExtra("notificationSenderId", originalSenderId)
        }

        val openChatPendingIntent = PendingIntent.getActivity(
            context,
            originalSenderId.hashCode(),
            openChatIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create RemoteInput for additional replies
        val replyLabel = "Reply to $originalSenderName"
        val remoteInput = RemoteInput.Builder(MyFirebaseService.KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        // Create intent for another reply
        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = MyFirebaseService.REPLY_ACTION
            putExtra("senderId", currentUserId) // Current user
            putExtra("receiverId", originalSenderId) // Other user
            putExtra("senderName", currentUserName) // Current user name
            putExtra("receiverName", originalSenderName) // Other user name
            putExtra("senderFcmToken", senderFcmToken) // You need to pass these from the original call
            putExtra("receiverFcmToken", receiverFcmToken) // You need to pass these from the original call
        }

        // CRITICAL: Use unique request code for each reply to avoid intent conflicts
        val replyRequestCode = (System.currentTimeMillis() + originalSenderId.hashCode()).toInt()
        val replyPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                replyRequestCode,
                replyIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                replyRequestCode,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Create reply action
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val conversationText = MyFirebaseService.buildConversationText(originalSenderId)

        // Build updated notification showing the reply was sent
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(originalSenderName)
            .setContentText(replyText) // Show user's reply
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openChatPendingIntent)
            .setAutoCancel(false) // Don't auto-cancel
            .addAction(replyAction)
            .setOnlyAlertOnce(true) // Don't alert for user's own reply
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(conversationText)
            )

        val notificationId = chatId.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }



    /**
     * Show error notification when reply fails
     */
    private fun showErrorNotification(context: Context, errorMessage: String = "Unknown error") {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reply_errors"

        // Create error notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reply Errors",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Errors when sending replies"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle("Reply Failed")
            .setContentText("Failed to send your reply: $errorMessage")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds

        notificationManager.notify(
            "reply_error_${System.currentTimeMillis()}".hashCode(),
            builder.build()
        )
    }
}