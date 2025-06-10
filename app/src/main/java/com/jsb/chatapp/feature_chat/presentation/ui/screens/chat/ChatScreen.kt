package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    currentUser: User,
    otherUser: User,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    // Initialize chat when screen starts
    LaunchedEffect(currentUser.uid, otherUser.uid) {
        viewModel.initChat(currentUser.uid, otherUser.uid)
    }

    // Top App Bar
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = otherUser.username,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface, // Use onSurface for default background
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable{ navController.navigateUp() }
                            .padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        AsyncImage(
                            model = otherUser.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            placeholder = painterResource(R.drawable.placeholder_profile),
                            error = painterResource(R.drawable.placeholder_profile)
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier.background(color = Color.Transparent)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
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

            // Message input + send button
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = state.messageInput,
                    onValueChange = { viewModel.onEvent(ChatEvent.OnMessageInputChanged(it)) },
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                )
                Button(onClick = { viewModel.onEvent(ChatEvent.OnSendMessage) }) {
                    Text("Send")
                }
            }
        }

        // Mark messages as seen once loaded
        LaunchedEffect(state.messages) {
            viewModel.onEvent(ChatEvent.OnMarkMessagesSeen)
        }
    }
}


