package com.jsb.chatapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatHomeScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.profile.ProfileScreen
import com.jsb.chatapp.theme.ChatAppTheme
import com.jsb.chatapp.util.SharedChatUserViewModel
import dagger.hilt.android.AndroidEntryPoint

//@AndroidEntryPoint
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            ChatAppTheme {
//                val navController = rememberNavController()
//
//                NavHost(navController = navController, startDestination = Screen.Splash.route) {
//                    composable(Screen.Splash.route) {
//                        SplashScreen(navController)
//                    }
//                    composable(Screen.Signin.route) {
//                        SignInScreen(navController)
//                    }
//                    composable(Screen.Signup.route) {
//                        SignupScreen(navController)
//                    }
//                    composable(Screen.Chat.route) { backStackEntry ->
//                        ChatScreen(
//                            chatId = backStackEntry.arguments?.getString("chatId") ?: "",
//                            navController = navController
//                        )
//                    }
//                }
//            }
//        }
//    }
//}


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChatAppTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route
                        ) {
                            composable(Screen.Splash.route) {
                                SplashScreen(navController, snackbarHostState)
                            }

                            composable(Screen.Signin.route) {
                                SignInScreen(navController)
                            }

                            composable(Screen.Signup.route) {
                                SignupScreen(navController)
                            }

                            composable(route = Screen.ChatHome.route) {
                                ChatHomeScreen(navController = navController)
                            }

                            composable(route = Screen.Profile.route) {
                                ProfileScreen(navController = navController)
                            }

                            composable(Screen.Chat.route) {
                                val parentEntry = remember {
                                    navController.getBackStackEntry(Screen.ChatHome.route)
                                }
                                val sharedUserViewModel: SharedChatUserViewModel = hiltViewModel(parentEntry)

                                val currentUser = sharedUserViewModel.currentUser
                                val otherUser = sharedUserViewModel.otherUser

                                if (currentUser != null && otherUser != null) {
                                    val chatId = listOf(currentUser.uid, otherUser.uid).sorted().joinToString("_")

                                    ChatScreen(
                                        chatId = chatId,
                                        currentUser = currentUser,
                                        otherUser = otherUser,
                                        navController = navController
                                    )
                                } else {
                                    Text("Invalid chat data")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}