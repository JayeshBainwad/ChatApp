//package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Send
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextField
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.rememberNavController
//import com.jsb.chatapp.theme.ChatAppTheme
//
//@Composable
//fun ChatScreen(
//    chatId: String,
//    navController: NavHostController,
//    viewModel: ChatViewModel = hiltViewModel()
//) {
//    val state by viewModel.state.collectAsState()
//    var messageText by remember { mutableStateOf("") }
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        LazyColumn(
//            modifier = Modifier
//                .weight(1f)
//                .padding(8.dp),
//            reverseLayout = true
//        ) {
//            items(state.messages) { message ->
//                Text(
//                    text = "${message.senderId}: ${message.text}",
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(4.dp)
//                )
//            }
//        }
//        Row(modifier = Modifier.padding(8.dp)) {
//            TextField(
//                value = messageText,
//                onValueChange = { messageText = it },
//                modifier = Modifier.weight(1f),
//                placeholder = { Text("Type a message") }
//            )
//            IconButton(onClick = {
//                if (messageText.isNotBlank()) {
//                    viewModel.onEvent(ChatEvent.SendMessage(messageText))
//                    messageText = ""
//                }
//            }) {
//                Icon(Icons.Default.Send, contentDescription = "Send")
//            }
//        }
//        if (state.isLoading) {
//            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
//        }
//        state.error?.let {
//            Text(
//                text = it,
//                color = MaterialTheme.colorScheme.error,
//                modifier = Modifier.padding(8.dp)
//            )
//        }
//    }
//}
//
//@Preview
//@Composable
//fun ChatScreenPreview() {
//    ChatAppTheme {
//        ChatScreen(chatId = "test", navController = rememberNavController())
//    }
//}

package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.theme.ChatAppTheme

@Composable
fun ChatScreen(
    chatId: String,
    navController: NavHostController
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Chat Screen (chatId: $chatId) - To be implemented")
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    ChatAppTheme {
        ChatScreen(chatId = "test", navController = rememberNavController())
    }
}