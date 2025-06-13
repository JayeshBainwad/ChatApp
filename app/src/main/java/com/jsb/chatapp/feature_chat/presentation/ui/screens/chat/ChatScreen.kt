package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.util.rememberImeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatId: String,
    currentUser: User,
    otherUser: User,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get keyboard state
    val imeState = rememberImeState()
    val isKeyboardOpen by imeState.isOpen

    // Track focus state and keyboard animation
    var isTextFieldFocused by remember { mutableStateOf(false) }
    val keyboardAnimationProgress = remember { Animatable(0f) }

    // Track previous keyboard state to detect transitions
    var previousKeyboardState by remember { mutableStateOf(false) }

    // Initialize chat when screen starts
    LaunchedEffect(currentUser.uid, otherUser.uid) {
        viewModel.initChat(currentUser.uid, otherUser.uid)
    }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                delay(80) // Slight delay for synchronization

                // Use smooth animation for new messages
                val targetIndex = state.messages.size - 1

                // Dynamic scrollOffset calculation for optimal positioning
                val scrollOffset = when {
                    // If we have many messages, scroll with more padding
                    state.messages.size > 10 -> -300

                    // For fewer messages, use moderate padding
                    state.messages.size > 5 -> -200

                    // For very few messages, minimal padding
                    else -> -100
                }

                listState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = scrollOffset
                )
            }
        }
    }

    // Smart scrollOffset approach with dynamic calculation
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen != previousKeyboardState && state.messages.isNotEmpty()) {
            previousKeyboardState = isKeyboardOpen

            if (isKeyboardOpen) {
                coroutineScope.launch {
                    delay(100) // Slight delay for synchronization

                    val targetIndex = state.messages.size - 1

                    // Dynamic scrollOffset calculation for optimal positioning
                    val scrollOffset = when {
                        // If we have many messages, scroll with more padding
                        state.messages.size > 10 -> -300

                        // For fewer messages, use moderate padding
                        state.messages.size > 5 -> -200

                        // For very few messages, minimal padding
                        else -> -100
                    }

                    listState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = scrollOffset
                    )
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
        // Messages List - This will automatically adjust its height
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f) // This makes it take remaining space
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            reverseLayout = false
        ) {
            items(state.messages) { message ->
                val isOwnMessage = message.senderId == currentUser.uid
                Row(
                    modifier = Modifier
                        .background(color = Color.Transparent)
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier
                            .background(
                                color = if (isOwnMessage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            )
                            .wrapContentSize()
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOwnMessage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Column {
                            Text(
                                text = message.content,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            if (isOwnMessage) {
                                Text(
                                    text = message.status.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .background(color = Color.Transparent)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = state.messageInput,
                    onValueChange = { viewModel.onEvent(ChatEvent.OnMessageInputChanged(it)) },
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isTextFieldFocused = focusState.isFocused
                        },
                    shape = RoundedCornerShape(24.dp),
                    placeholder = {
                        if (!isTextFieldFocused) {
                            Text(
                                text = "Type a message...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        cursorColor = colors.primary,
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedLeadingIconColor = colors.primary,
                        unfocusedLeadingIconColor = colors.primary,
                        disabledLeadingIconColor = colors.primary,
                        focusedTrailingIconColor = colors.primary,
                        unfocusedTrailingIconColor = colors.primary,
                        disabledTrailingIconColor = colors.primary
                    ),
                    maxLines = 4,
                    singleLine = false
                )

                IconButton(
                    onClick = {
                        if (state.messageInput.isNotBlank()) {
                            viewModel.onEvent(ChatEvent.OnSendMessage)
                        }
                    },
                    modifier = Modifier
                        .padding(start = 2.dp, bottom = 2.dp, end = 2.dp)
                        .size(30.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.send),
                        contentDescription = "Send",
                        tint = colors.primary
                    )
                }
            }
        }
    }

    // Mark messages as seen once loaded
    LaunchedEffect(state.messages) {
        viewModel.onEvent(ChatEvent.OnMarkMessagesSeen)
    }
}