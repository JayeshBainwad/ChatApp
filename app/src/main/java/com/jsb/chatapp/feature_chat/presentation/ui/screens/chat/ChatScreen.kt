package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_chat.data.fcm.NotificationReplyReceiver
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.components.MessageCard
import com.jsb.chatapp.feature_chat.presentation.util.rememberImeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    currentUser: User,
    otherUser: User,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatId = otherUser.uid + currentUser.uid
    val state = viewModel.uiState
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get keyboard state
    val imeState = rememberImeState()
    val isKeyboardOpen by imeState.isOpen

    // Track focus state for UI behavior
    var isTextFieldFocused by remember { mutableStateOf(false) }

    // Track previous keyboard state to detect transitions
    var previousKeyboardState by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Clear notification when chat screen opens
    LaunchedEffect(Unit) {
        NotificationReplyReceiver.clearConversationAndNotification(context, chatId)
    }

    LaunchedEffect(currentUser.uid, otherUser.uid) {
        viewModel.initChat(
            currentUserId = currentUser.uid,
            currentUserName = currentUser.username,
            otherUserId = otherUser.uid,
            otherUserFcmToken = otherUser.fcmToken,
            currentUserFcmToken = currentUser.fcmToken,
            otherUserName = otherUser.username
        )
    }

    // Unified scroll function to avoid duplication
    suspend fun scrollToBottom(isKeyboardTriggered: Boolean = false) {
        delay(100)
        if (state.messages.isNotEmpty()) {
            val targetIndex = state.messages.size - 1

            // Dynamic scrollOffset calculation
            val scrollOffset = when {
                state.messages.size > 10 -> if (isKeyboardTriggered) -350 else -200
                state.messages.size > 5 -> if (isKeyboardTriggered) -250 else -150
                else -> if (isKeyboardTriggered) -150 else -100
            }

            // Add slight delay for keyboard-triggered scrolls
            if (isKeyboardTriggered) {
                delay(100) // Sync with keyboard animation
            }

            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = scrollOffset
            )
        }
    }

    // Auto scroll when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            // Only scroll if we're not currently handling keyboard opening
            if (!isKeyboardOpen || previousKeyboardState == isKeyboardOpen) {
                coroutineScope.launch {
                    scrollToBottom(isKeyboardTriggered = false)
                }
            }
        }
    }

    // Handle keyboard opening/closing
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen != previousKeyboardState && state.messages.isNotEmpty()) {
            previousKeyboardState = isKeyboardOpen

            if (isKeyboardOpen) {
                coroutineScope.launch {
                    scrollToBottom(isKeyboardTriggered = true)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            reverseLayout = false
        ) {
            items(state.messages) { message ->
                val isOwnMessage = message.senderId == currentUser.uid
                MessageCard(
                    message = message,
                    isOwnMessage = isOwnMessage
                )
            }
        }

        // Input Section
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = state.messageInput,
                    onValueChange = { viewModel.onEvent(ChatEvent.OnMessageInputChanged(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isTextFieldFocused = focusState.isFocused
                        },
                    shape = RoundedCornerShape(24.dp),
                    placeholder = {
                        Text(
                            text = "Type a message...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        cursorColor = colors.primary,
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4,
                    singleLine = false
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(56.dp)
                        .width(40.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (state.messageInput.isNotBlank()) {
                                viewModel.onEvent(ChatEvent.OnSendMessage)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.send),
                            contentDescription = "SEND",
                            tint = colors.primary
                        )
                    }
                }
            }
        }
    }

    // Mark messages as seen once loaded
    LaunchedEffect(state.messages) {
        viewModel.onEvent(ChatEvent.OnMarkMessagesSeen)
    }
}

