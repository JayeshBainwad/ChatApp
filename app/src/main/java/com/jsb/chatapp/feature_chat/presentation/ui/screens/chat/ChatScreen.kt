package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.jsb.chatapp.Screen
import com.jsb.chatapp.theme.ChatAppTheme

@Composable
fun ChatScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val firestoreUser = viewModel.firestoreUser.value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Chat Screen (chatId: $chatId) - To be implemented")

            Spacer(modifier = Modifier.height(16.dp))

            firestoreUser?.let { user ->
                if (user.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = user.username, // username from Firestore
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.name, // actual name
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                if (!user.bio.isNullOrBlank()) {
                    Text(
                        text = user.bio,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } ?: run {
                Text("Loading user data...", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                viewModel.logout {
                    navController.navigate(Screen.Signin.route) {
                        popUpTo(Screen.Chat.route) { inclusive = true } // Prevent back navigation
                    }
                }
            }) {
                Text("Sign out")
            }
        }
    }
}


@Preview
@Composable
fun ChatScreenPreview() {
    ChatAppTheme {
        ChatScreen(chatId = "test", navController = rememberNavController())
    }
}