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
    private lateinit var otherUserFcmToken: String
    private lateinit var otherUserId: String

    fun initChat(currentUserId: String, currentUserName: String, otherUserId: String, otherUserFcmToken: String) {
        this.currentUserId = currentUserId
        this.currentUserName = currentUserName
        this.otherUserId = otherUserId
        this.otherUserFcmToken = otherUserFcmToken
        this.chatId = generateChatId(currentUserId, otherUserId)

        repository.listenForMessages(chatId) { messages ->
            uiState = uiState.copy(messages = messages)
        }
    }


    private fun generateChatId(user1: String, user2: String): String {
        return listOf(user1, user2).sorted().joinToString("_")
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
                        status = MessageStatus.SENT
                    )
                    viewModelScope.launch {
                        repository.sendMessage(chatId, message)
                    }

                    if (otherUserFcmToken.isNotEmpty() && otherUserFcmToken.isNotBlank()) {

                        FCMSender.sendPushNotification(
                            serverUrl = "http://192.168.0.109:8080/send-notification",
                            fcmToken = otherUserFcmToken,
                            senderName = currentUserName,
                            senderId = currentUserId,
                            receiverId = otherUserId, // not used
                            content = message.content
                        )
                    } else {
                        Log.d("FCM_DEBUG", "FCM token is empty or blank")
                    }


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

    object FCMSender {
        fun sendPushNotification(
            serverUrl: String,
            fcmToken: String,
            senderId: String,
            receiverId: String, // not used
            senderName: String,
            content: String
        ) {
            val client = OkHttpClient()

            val json = JSONObject().apply {
                put("fcmToken", fcmToken)
                put("senderId", senderId)
                put("receiverId", receiverId) // not used
                put("senderName", senderName)
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
