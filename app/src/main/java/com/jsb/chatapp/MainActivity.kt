package com.jsb.chatapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.MainWithBarsScreen
import com.jsb.chatapp.theme.ChatAppTheme
import com.jsb.chatapp.util.SharedChatUserViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // IMPORTANT: Use ONLY one of these approaches, not both
        // Option 1: Traditional approach (RECOMMENDED for your case)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Option 2: Modern edge-to-edge approach (comment out if using Option 1)
         WindowCompat.setDecorFitsSystemWindows(window, false)

        // Optional: Set status bar to transparent for modern look
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            ChatAppTheme {
                val rootNavController = rememberNavController()
                val mainNavController = rememberNavController()

                val snackbarHostState = remember { SnackbarHostState() }

                NavHost(
                    navController = rootNavController,
                    startDestination = Screen.Splash.route
                ) {
                    // 1. Root Graph
                    composable(Screen.Splash.route) {
                        SplashScreen(rootNavController, snackbarHostState) //No value passed for parameter 'snackbarHostState'
                    }
                    composable(Screen.Signin.route) {
                        SignInScreen(rootNavController)
                    }
                    composable(Screen.Signup.route) {
                        SignupScreen(rootNavController)
                    }

                    // 2. Main Graph Wrapper
                    composable(Screen.Main.route) {
                        // Create SharedChatUserViewModel at this level (Main entry)
                        val sharedUserViewModel: SharedChatUserViewModel = hiltViewModel()
                        MainWithBarsScreen(
                            rootNavController = rootNavController,
                            sharedUserViewModel = sharedUserViewModel
                        )
                    }

                    // 3. Fullscreen Chat
                    composable(Screen.Chat.route) {
                        val parentEntry = remember {
                            rootNavController.getBackStackEntry(Screen.Main.route)
                        }
                        val sharedViewModel: SharedChatUserViewModel = hiltViewModel(parentEntry)

                        val currentUser = sharedViewModel.currentUser
                        val otherUser = sharedViewModel.otherUser

                        // Ensure both users are not null before rendering ChatScreen
                        if (currentUser != null && otherUser != null) {
                            val chatId = listOf(currentUser.uid, otherUser.uid).sorted().joinToString("_")

                            ChatScreen(
                                chatId = chatId,
                                currentUser = currentUser,
                                otherUser = otherUser,
                                navController = rootNavController
                            )
                        } else {
                            // Show error state with better UX
                            Text(text = "Loading chat... Please wait.")
                        }
                    }
                }
            }
        }
    }
}