package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.components.CustomSearchBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.components.CustomUserCard
import com.jsb.chatapp.theme.ChatAppTheme
import com.jsb.chatapp.util.SharedChatUserViewModel

@SuppressLint("UnrememberedMutableState", "UnrememberedGetBackStackEntry")
@Composable
fun ChatHomeScreen(
    navController: NavHostController,
    viewModel: ChatHomeViewModel = hiltViewModel()
) {
    val otherUserState = viewModel.state // ✅ Correct
    val currentUser = viewModel.firestoreUser.value
    val currentUserId = currentUser?.uid
    val parentEntry = remember {
        navController.getBackStackEntry(Screen.ChatHome.route)
    }
    val sharedUserViewModel: SharedChatUserViewModel = hiltViewModel(parentEntry)


    LaunchedEffect(currentUserId) {
        currentUserId?.let {
            viewModel.loadChats(it)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(otherUserState.users) { otherUser ->
                            if (otherUser.uid != currentUserId) {
                                CustomUserCard(
                                    otherUserId = otherUser.uid,
                                    otherUserName = otherUser.username,
                                    otherUserAvatar = otherUser.avatarUrl,
                                    otherUserPhoneNumber = otherUser.phoneNumber
                                ) {
                                    currentUser?.let { current ->
                                        sharedUserViewModel.setUsers(current = current, other = otherUser)
                                        navController.navigate(Screen.Chat.route)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Showing recent chats
                    val chats = viewModel.chatList.value


                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(chats) { chat ->
                            CustomUserCard(
                                otherUserAvatar = chat.otherUser.avatarUrl,
                                otherUserPhoneNumber = chat.otherUser.phoneNumber,
                                otherUserName = chat.otherUser.username,
                                otherUserId = chat.otherUser.uid
                            ) {
                                currentUser?.let { current ->
                                    sharedUserViewModel.setUsers(current = current, other = chat.otherUser)
                                    navController.navigate(Screen.Chat.route)
                                }
                            }
                        }
                    }
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ){
                Button(
                    onClick = {
                        viewModel.logout {
                            navController.navigate(Screen.Signin.route) { popUpTo(0) }
                        }
                    },
                    modifier = Modifier

                ) {
                    Text("Sign out")
                }
                Button(
                    onClick = {
                        navController.navigate(Screen.Profile.route)
                    },
                    modifier = Modifier

                ) {
                    Text("Profile")
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


@Preview
@Composable
fun ChatScreenPreview() {
    ChatAppTheme {
        ChatHomeScreen(navController = rememberNavController())
    }
}