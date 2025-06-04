package com.jsb.chatapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.GoogleAuthUiClient
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.theme.ChatAppTheme
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

                            composable(Screen.Chat.route) { backStackEntry ->
                                ChatScreen(
                                    chatId = backStackEntry.arguments?.getString("chatId") ?: "default",
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
