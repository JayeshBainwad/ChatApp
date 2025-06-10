package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

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
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private lateinit var chatId: String
    private lateinit var currentUserId: String
    private lateinit var otherUserId: String

    fun initChat(currentUser: String, otherUser: String) {
        this.currentUserId = currentUser
        this.otherUserId = otherUser
        this.chatId = generateChatId(currentUser, otherUser)

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
                        status = MessageStatus.SENT
                    )
                    viewModelScope.launch {
                        repository.sendMessage(chatId, message)
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
}
