package com.jsb.chatapp.feature_chat.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jsb.chatapp.feature_core.core_navigation.MainActivity
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_chat.data.fcm.NotificationReplyReceiver.Companion.generateChatId
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
import com.jsb.chatapp.feature_chat.domain.usecase.UpdateFcmTokenUseCase
import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var updateFcmTokenUseCase: UpdateFcmTokenUseCase

    @Inject
    lateinit var chatRepository: ChatRepository

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val REPLY_ACTION = "com.jsb.chatapp.REPLY_ACTION"
        private val channelId = "chat_notifications"
        private var isNotificationCreated = false

        // Store conversation messages for persistent notification display
        private val conversationMessages = mutableMapOf<String, MutableList<ConversationMessage>>()

        // Track pending messages for smart notifications (same as NotificationReplyReceiver)
        private val pendingNotificationMessages = mutableMapOf<String, ReceivedMessageData>()

        /**
         * Clear conversation messages for a sender (static version)
         */
        fun clearConversationStatic(senderId: String) {
            isNotificationCreated = false
            conversationMessages.remove(senderId)
            // Also clear any pending notifications for this sender
            pendingNotificationMessages.values.removeAll { it.senderId == senderId }
        }

        /**
         * Add a user's reply to the conversation history (static version)
         */
        fun addUserReplyToConversationStatic(senderId: String, replyContent: String, senderName: String) {
            if (!conversationMessages.containsKey(senderId)) {
                conversationMessages[senderId] = mutableListOf()
            }

            conversationMessages[senderId]?.add(
                ConversationMessage(
                    sender = senderName,
                    message = replyContent,
                    timestamp = System.currentTimeMillis(),
                    isFromUser = true
                )
            )
        }

        /**
         * Build conversation text for notification display
         * Shows last 6 messages in WhatsApp style
         */
        fun buildConversationText(senderId: String): String {
            val messages = conversationMessages[senderId] ?: return ""
            val sortedMessages = messages.sortedBy { it.timestamp }

            val conversation = StringBuilder()
            var lastSender = ""

            // Show last 6 messages for context
            sortedMessages.takeLast(6).forEach { msg ->
                if (msg.sender != lastSender) {
                    if (conversation.isNotEmpty()) conversation.append("\n")
                    conversation.append("${msg.sender}: ${msg.message}")
                    lastSender = msg.sender
                } else {
                    conversation.append("\n${msg.message}")
                }
            }

            return conversation.toString()
        }
    }

    /**
     * Data class to store individual conversation messages
     */
    data class ConversationMessage(
        val sender: String,
        val message: String,
        val timestamp: Long,
        val isFromUser: Boolean = false
    )

    /**
     * Data class to store received message data for pending notifications
     */
    data class ReceivedMessageData(
        val senderName: String,
        val receiverName: String,
        val content: String,
        val senderId: String,
        val receiverId: String,
        val senderFcmToken: String,
        val receiverFcmToken: String,
        val messageId: String,
        val timestamp: Long
    )

    /**
     * Called when FCM token is refreshed - update token in backend
     */
    override fun onNewToken(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("FCM", "New FCM token generated: $token")

        CoroutineScope(Dispatchers.IO).launch {
            updateFcmTokenUseCase(userId, token)
        }
    }

    /**
     * Called when FCM message is received
     * Uses smart notification logic - waits to see if message is seen before showing notification
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM_RECEIVED", "Message received from: ${remoteMessage.from}")
        Log.d("FCM_RECEIVED", "Message data: ${remoteMessage.data}")

        // Extract message data
        val senderName = remoteMessage.data["senderName"] // otherUserName
        val receiverName = remoteMessage.data["receiverName"] // currentUserName
        val content = remoteMessage.data["content"]
        val senderId = remoteMessage.data["senderId"]
        val receiverId = remoteMessage.data["receiverId"]
        val senderFcmToken = remoteMessage.data["currentUserFcmToken"]
        val receiverFcmToken = remoteMessage.data["otherUserFcmToken"]

        // Validate all required fields are present
        if (senderName != null && receiverName != null && content != null && senderId != null &&
            senderFcmToken != null && receiverId != null && receiverFcmToken != null) {

            // Generate a unique message ID for tracking
            val messageId = "${senderId}_${System.currentTimeMillis()}"

            // Add the received message to conversation history immediately
            addMessageToConversation(senderId, senderName, content)

            // Create received message data for smart notification tracking
            val receivedMessageData = ReceivedMessageData(
                senderName = senderName,
                receiverName = receiverName,
                content = content,
                senderId = senderId,
                receiverId = receiverId,
                senderFcmToken = senderFcmToken,
                receiverFcmToken = receiverFcmToken,
                messageId = messageId,
                timestamp = System.currentTimeMillis()
            )

            // Start smart notification monitoring (same logic as NotificationReplyReceiver)
            startMessageStatusMonitoring(receivedMessageData)

        } else {
            Log.w("FCM_RECEIVED", "Missing fields in notification data")
        }
    }

    /**
     * Smart notification monitoring - same logic as NotificationReplyReceiver
     * Waits 2 seconds, then checks if message was seen before showing notification
     */
    private fun startMessageStatusMonitoring(messageData: ReceivedMessageData) {
        // Add to pending notifications
        pendingNotificationMessages[messageData.messageId] = messageData

        CoroutineScope(Dispatchers.IO).launch {
            // Start listening for message status changes for this chat
            listenForMessageStatusChanges(messageData.senderId, messageData.receiverId)

            // Wait for 2 seconds
            delay(2000L)

            // Check if message is still pending (not seen)
            val pendingMessage = pendingNotificationMessages[messageData.messageId]
            if (pendingMessage != null) {
                Log.d("SMART_NOTIFICATION", "Message ${messageData.messageId} not seen after 2 seconds, showing notification")

                // Create notification channel if needed
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if(!isNotificationCreated){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            channelId,
                            "Chat Notifications",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "Notifications for chat messages"
                            enableVibration(true)
                        }
                        notificationManager.createNotificationChannel(channel)
                        isNotificationCreated = true
                    }
                }

                // Show notification with reply functionality
                showNotificationWithReply(
                    senderName = pendingMessage.senderName,
                    receiverName = pendingMessage.receiverName,
                    content = pendingMessage.content,
                    senderId = pendingMessage.senderId,
                    receiverId = pendingMessage.receiverId,
                    senderFcmToken = pendingMessage.senderFcmToken,
                    receiverFcmToken = pendingMessage.receiverFcmToken
                )

                // Remove from pending after showing notification
                pendingNotificationMessages.remove(messageData.messageId)
            } else {
                Log.d("SMART_NOTIFICATION", "Message ${messageData.messageId} was seen, skipping notification")
            }
        }
    }

    /**
     * Listen for message status changes to detect when messages are seen
     */
    private fun listenForMessageStatusChanges(senderId: String, receiverId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val chatId = generateChatId(senderId, receiverId)
            chatRepository.listenForMessages(chatId) { messages ->
                checkMessageStatusChanges(messages)
            }
        }
    }

    /**
     * Check if any pending messages have been seen and remove them from pending notifications
     */
    private fun checkMessageStatusChanges(currentMessages: List<Message>) {
        val currentMessagesMap = currentMessages.associateBy { it.messageId }

        // Check all pending messages for status changes
        pendingNotificationMessages.keys.toList().forEach { messageId ->
            val currentMessage = currentMessagesMap[messageId]

            if (currentMessage != null && currentMessage.status == MessageStatus.SEEN) {
                pendingNotificationMessages.remove(messageId)
                Log.d("MESSAGE_STATUS", "Received message $messageId was seen - notification cancelled")
            }
        }
    }

    /**
     * Add a received message to the conversation history for display in notification
     */
    private fun addMessageToConversation(senderId: String, senderName: String, content: String) {
        if (!conversationMessages.containsKey(senderId)) {
            conversationMessages[senderId] = mutableListOf()
        }

        conversationMessages[senderId]?.add(
            ConversationMessage(
                sender = senderName,
                message = content,
                timestamp = System.currentTimeMillis(),
                isFromUser = false
            )
        )
    }

    /**
     * Fetch unread messages from Firestore to build complete conversation history
     */
    private suspend fun fetchUnreadMessages(
        senderId: String,
        receiverId: String
    ): List<ConversationMessage> {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val chatId = generateChatId(senderId, receiverId)
            val snapshot = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", MessageStatus.SENT.name)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val content = doc.getString("content") ?: ""
                val senderName = doc.getString("senderName") ?: ""
                val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                ConversationMessage(
                    sender = senderName,
                    message = content,
                    timestamp = timestamp,
                    isFromUser = false
                )
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error fetching unread messages", e)
            emptyList()
        }
    }

    /**
     * Create and display notification with reply functionality
     */
    private suspend fun showNotificationWithReply(
        senderName: String,
        receiverName: String,
        content: String,
        senderId: String,
        receiverId: String,
        senderFcmToken: String,
        receiverFcmToken: String
    ) {
        // Fetch unread messages and ADD them to existing conversation (don't replace)
        val unreadMessages = fetchUnreadMessages(senderId, receiverId)

        // Get existing conversation or create new one
        if (!conversationMessages.containsKey(senderId)) {
            conversationMessages[senderId] = mutableListOf()
        }

        // Add unread messages that aren't already in the conversation
        val existingMessages = conversationMessages[senderId]!!
        val existingTimestamps = existingMessages.map { it.timestamp }.toSet()

        unreadMessages.forEach { unreadMsg ->
            if (unreadMsg.timestamp !in existingTimestamps) {
                existingMessages.add(unreadMsg)
            }
        }

        // Sort messages by timestamp and update the map
        conversationMessages[senderId] = existingMessages.sortedBy { it.timestamp }.toMutableList()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chatId = senderId + receiverId

        // Create intent to open chat when notification is tapped
        val openChatIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateToChat", true)
            // Note: When opening chat, current user becomes sender, other user becomes receiver
            putExtra("senderId", receiverId) // Current user ID
            putExtra("receiverId", senderId) // Other user ID
            putExtra("senderName", receiverName) // Current user name
            putExtra("receiverName", senderName) // Other user name
            putExtra("senderFcmToken", receiverFcmToken) // Current user FCM token
            putExtra("receiverFcmToken", senderFcmToken) // Other user FCM token
            putExtra("clearNotification", true)
            putExtra("notificationSenderId", senderId)
        }

        val openChatPendingIntent = PendingIntent.getActivity(
            this,
            senderId.hashCode(),
            openChatIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create RemoteInput for reply functionality
        val replyLabel = "Reply to $senderName"
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        // Create intent for reply action
        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            action = REPLY_ACTION
            // For reply: current user (receiver) becomes sender, original sender becomes receiver
            putExtra("senderId", receiverId) // Current user ID (will send the reply)
            putExtra("receiverId", senderId) // Original sender ID (will receive the reply)
            putExtra("senderName", receiverName) // Current user name
            putExtra("receiverName", senderName) // Original sender name
            putExtra("senderFcmToken", receiverFcmToken) // Current user FCM token
            putExtra("receiverFcmToken", senderFcmToken) // Original sender FCM token
        }

        val replyRequestCode = (System.currentTimeMillis() + senderId.hashCode()).toInt()
        val replyPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                this,
                replyRequestCode,
                replyIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                replyRequestCode,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Create reply action button
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        // Build conversation text for WhatsApp-style display
        val conversationText = buildConversationText(senderId)

        // Build and show notification
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(senderName)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openChatPendingIntent)
            .setAutoCancel(false) // Don't auto-cancel
            .addAction(replyAction)
            .setOnlyAlertOnce(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(conversationText)
            )

        val notificationId = chatId.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }
}