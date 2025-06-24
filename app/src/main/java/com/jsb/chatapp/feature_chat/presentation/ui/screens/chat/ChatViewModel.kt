package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private lateinit var chatId: String
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    private lateinit var otherUserName: String
    private lateinit var otherUserFcmToken: String
    private lateinit var currentUserFcmToken: String
    private lateinit var otherUserId: String

    // Track sent messages to check their status later
    private val sentMessageIds = mutableSetOf<String>()

    // Track messages that have been sent for delayed notification
    private val pendingNotificationMessages = mutableMapOf<String, Message>()

    fun initChat(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserFcmToken: String,
        currentUserFcmToken: String,
        otherUserName: String
    ) {
        this.currentUserId = currentUserId
        this.currentUserName = currentUserName
        this.otherUserName = otherUserName
        this.otherUserId = otherUserId
        this.otherUserFcmToken = otherUserFcmToken
        this.currentUserFcmToken = currentUserFcmToken
        this.chatId = generateChatId(currentUserId, otherUserId)

        repository.listenForMessages(chatId) { messages ->
            val previousMessages = uiState.messages
            uiState = uiState.copy(messages = messages)

            // Check for status changes in sent messages
            checkMessageStatusChanges(previousMessages, messages)
        }
    }

    private fun checkMessageStatusChanges(
        previousMessages: List<Message>,
        currentMessages: List<Message>
    ) {
        val currentMessagesMap = currentMessages.associateBy { it.messageId }

        // Check all pending messages for status changes
        pendingNotificationMessages.keys.toList().forEach { messageId ->
            val currentMessage = currentMessagesMap[messageId]

            if (currentMessage != null) {
                // If message status changed to SEEN, remove from pending
                if (currentMessage.status == MessageStatus.SEEN) {
                    pendingNotificationMessages.remove(messageId)
                    sentMessageIds.remove(messageId)
                    Log.d("MESSAGE_STATUS", "Message $messageId was seen - notification cancelled")
                }
            }
        }

        // Remove from sentMessageIds if message was seen
        sentMessageIds.toList().forEach { messageId ->
            val currentMessage = currentMessagesMap[messageId]
            if (currentMessage?.status == MessageStatus.SEEN) {
                sentMessageIds.remove(messageId)
            }
        }
    }

    private fun scheduleDelayedNotification(message: Message) {
        // Add to pending notifications
        pendingNotificationMessages[message.messageId] = message

        viewModelScope.launch {
            // Wait for 2 seconds
            delay(2000L)

            // Check if message is still pending (not seen)
            val pendingMessage = pendingNotificationMessages[message.messageId]
            if (pendingMessage != null &&
                otherUserFcmToken.isNotEmpty() &&
                otherUserFcmToken.isNotBlank()) {

                Log.d("SMART_NOTIFICATION", "Message ${message.messageId} not seen after 3 seconds, sending notification")

                FCMSender.sendPushNotification(
//                    serverUrl = "http://192.168.0.109:8080/send-notification",
                    serverUrl = "https://ktor-fcm-notification-chatapp.onrender.com/send-notification",
                    otherUserFcmToken = otherUserFcmToken,
                    currentUserFcmToken = currentUserFcmToken,
                    senderName = currentUserName,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    content = message.content,
                    receiverName = otherUserName
                )

                // Remove from pending after sending notification
                pendingNotificationMessages.remove(message.messageId)
            }
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnMessageInputChanged -> {
                uiState = uiState.copy(messageInput = event.input)
            }

            is ChatEvent.OnSendMessage -> {
                if (uiState.messageInput.isNotBlank()) {
                    val message = Message(
                        senderId = currentUserId,
                        receiverId = otherUserId,
                        content = uiState.messageInput,
                        senderName = currentUserName,
                        status = MessageStatus.SENT,
                        timestamp = System.currentTimeMillis()
                    )

                    viewModelScope.launch {
                        repository.sendMessage(chatId, message)
                    }

                    // Track this message for status monitoring
                    sentMessageIds.add(message.messageId)
                    Log.d("MESSAGE_TRACKING", "Tracking message ${message.messageId} for status changes")

                    // Schedule delayed notification for this message
                    scheduleDelayedNotification(message)

                    uiState = uiState.copy(messageInput = "")
                }
            }

            is ChatEvent.OnMarkMessagesSeen -> {
                val unseenMessageIds = uiState.messages.filter {
                    it.receiverId == currentUserId && it.status != MessageStatus.SEEN
                }.map { it.messageId }

                if (unseenMessageIds.isNotEmpty()) {
                    viewModelScope.launch {
                        repository.markMessagesSeen(chatId, unseenMessageIds)
                    }
                }
            }
        }
    }

    private fun generateChatId(user1: String, user2: String): String {
        return listOf(user1, user2).sorted().joinToString("_")
    }

    object FCMSender {
        fun sendPushNotification(
            serverUrl: String,
            otherUserFcmToken: String,
            currentUserFcmToken: String,
            senderId: String,
            receiverId: String,
            senderName: String,
            receiverName: String,
            content: String
        ) {
            val client = OkHttpClient()

            val json = JSONObject().apply {
                put("otherUserFcmToken", otherUserFcmToken)
                put("currentUserFcmToken", currentUserFcmToken)
                put("senderId", senderId)
                put("receiverId", receiverId)
                put("senderName", senderName)
                put("receiverName", receiverName)
                put("content", content)
            }

            Log.d("FCM_DEBUG", "Sending JSON: $json")

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FCM_DEBUG", "Failed to send push notification", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("FCM_DEBUG", "Push notification response: $responseBody")
                    if (!response.isSuccessful) {
                        Log.e("FCM_DEBUG", "Error sending push notification: HTTP ${response.code}")
                    }
                }
            })
        }
    }
}