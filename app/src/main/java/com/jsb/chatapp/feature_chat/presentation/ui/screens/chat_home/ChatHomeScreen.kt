package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.components.CustomSearchBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.components.CustomUserCard
import com.jsb.chatapp.theme.ChatAppTheme
import com.jsb.chatapp.util.SharedChatUserViewModel
import com.jsb.chatapp.util.formatChatTimestamp

@SuppressLint("UnrememberedMutableState", "UnrememberedGetBackStackEntry")
@Composable
fun ChatHomeScreen(
    rootNavController: NavController,
    sharedUserViewModel: SharedChatUserViewModel,
    viewModel: ChatHomeViewModel = hiltViewModel()
) {
    val otherUserState = viewModel.state // ✅ Correct
    val currentUser = viewModel.firestoreUser.value
    val currentUserId = currentUser?.uid
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUserId) {
        currentUserId?.let {
            viewModel.loadChats(it)
        }
    }

    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            CustomSearchBar(
                query = otherUserState.query,
                onQueryChange = { query ->
                    viewModel.onEvent(ChatHomeEvent.OnQueryChange(query))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (otherUserState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                if (otherUserState.query.isNotBlank()) {
                    // Searching users
                    LazyColumn(
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.background)
                    ) {
                        items(otherUserState.userChatInfos) { userChatInfo  ->
                            if (userChatInfo.user.uid != currentUserId) {
                                CustomUserCard(
                                    otherUserName = userChatInfo.user.username,
                                    otherUserAvatar = userChatInfo.user.avatarUrl,
                                    lastMessageTime = formatChatTimestamp(userChatInfo.timestamp),
                                    otherUserLastMessage = userChatInfo.lastMessage ?: ""

                                ) {
                                    currentUser?.let { current ->
                                        sharedUserViewModel.setUsers(current = current, other = userChatInfo.user)
                                        rootNavController.navigate(Screen.Chat.route)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Showing recent chats
                    val chats = viewModel.chatList.value
                    LazyColumn(
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.background)
                    ) {
                        items(chats) { chat ->
                            CustomUserCard(
                                otherUserAvatar = chat.otherUser.avatarUrl,
                                otherUserName = chat.otherUser.username,
                                lastMessageTime = formatChatTimestamp(chat.timestamp),
                                otherUserLastMessage = chat.lastMessage
                            ) {
                                currentUser?.let { current ->
                                    sharedUserViewModel.setUsers(current = current, other = chat.otherUser)
                                    rootNavController.navigate(Screen.Chat.route)
                                }
                            }
                        }
                    }
                }
            }
        }

        otherUserState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}