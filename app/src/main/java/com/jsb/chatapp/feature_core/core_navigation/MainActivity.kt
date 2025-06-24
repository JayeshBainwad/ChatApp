package com.jsb.chatapp.feature_core.core_navigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen
import com.jsb.chatapp.feature_chat.data.fcm.MyFirebaseService
import com.jsb.chatapp.feature_chat.data.fcm.NotificationReplyReceiver
import com.jsb.chatapp.feature_core.core_domain.main_usecase.GetCurrentUserUseCase
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.MainWithBarsScreen
import com.jsb.chatapp.feature_core.theme.ChatAppTheme
import com.jsb.chatapp.feature_core.main_util.AppLifecycleObserver
import com.jsb.chatapp.feature_core.main_util.RequestPermission
import com.jsb.chatapp.feature_core.main_util.Result
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle notification tap
        handleNotificationTap()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Register lifecycle observer
        lifecycle.addObserver(appLifecycleObserver)

        setContent {
            ChatAppTheme {
                RequestPermission()
                val rootNavController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                // ✅ Extract intent extras for notification handling
                // ✅ Extract intent extras for notification handling
                val shouldNavigate = intent?.getBooleanExtra("navigateToChat", false) ?: false
                val senderId = intent?.getStringExtra("senderId")
                val receiverId = intent?.getStringExtra("receiverId")
                val senderName = intent?.getStringExtra("senderName")
                val senderFcmToken = intent?.getStringExtra("senderFcmToken")
                val receiverFcmToken = intent?.getStringExtra("receiverFcmToken")

// ✅ Determine the "other user" based on current user
                val notificationOtherUser = remember {
                    if (shouldNavigate && senderId != null && receiverId != null && senderName != null && senderFcmToken != null) {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                        Log.d("FCM_DEBUG", "MainActivity - Current user ID: $currentUserId")
                        Log.d("FCM_DEBUG", "MainActivity - Sender ID: $senderId")
                        Log.d("FCM_DEBUG", "MainActivity - Receiver ID: $receiverId")

                        when (currentUserId) {
                            receiverId -> {
                                // Current user is the receiver, so sender is the other user
                                Log.d("FCM_DEBUG", "MainActivity - Current user is receiver, sender is other user")
                                User(
                                    uid = senderId,
                                    username = senderName,
                                    fcmToken = senderFcmToken
                                )
                            }
                            senderId -> {
                                // Current user is the sender, so receiver is the other user
                                // This case is less common for notifications but can happen
                                Log.d("FCM_DEBUG", "MainActivity - Current user is sender, receiver is other user")
                                // You'll need receiver name and fcm token from the intent
                                val receiverName = intent?.getStringExtra("receiverName")
                                if (receiverName != null && receiverFcmToken != null) {
                                    User(
                                        uid = receiverId,
                                        username = receiverName,
                                        fcmToken = receiverFcmToken
                                    )
                                } else {
                                    Log.w("FCM_DEBUG", "MainActivity - Missing receiver info")
                                    null
                                }
                            }
                            else -> {
                                Log.w("FCM_DEBUG", "MainActivity - Current user doesn't match sender or receiver")
                                null
                            }
                        }
                    } else {
                        Log.d("FCM_DEBUG", "MainActivity - Missing required notification data")
                        null
                    }
                }

                Log.d("FCM_DEBUG", "MainActivity - shouldNavigate: $shouldNavigate")
                Log.d("FCM_DEBUG", "MainActivity - notificationOtherUser: " +
                        "${notificationOtherUser?.username} (${notificationOtherUser?.uid})")
                Log.d("FCM_DEBUG", "MainActivity - senderId: $senderId")
                Log.d("FCM_DEBUG", "MainActivity - senderName: $senderName")

                // ✅ Handle notification click navigation - removed premature navigation
                LaunchedEffect(key1 = shouldNavigate, block = {
                    if (shouldNavigate && notificationOtherUser != null) {
                        // Check if user is authenticated
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUserId != null) {
                            Log.d("FCM_DEBUG", "MainActivity - User authenticated, will navigate to main")
                            // Navigate to main screen - let MainWithBarsScreen handle the chat navigation
                            rootNavController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            Log.d("FCM_DEBUG", "MainActivity - User not authenticated")
                        }
                    }
                })

                NavHost(
                    navController = rootNavController,
                    startDestination = Screen.Splash.route
                ) {
                    // 1. Root Graph
                    composable(Screen.Splash.route) {
                        SplashScreen(rootNavController, snackbarHostState)
                    }
                    composable(Screen.Signin.route) {
                        SignInScreen(rootNavController)
                    }
                    composable(Screen.Signup.route) {
                        SignupScreen(rootNavController)
                    }

                    // 2. Main Graph Wrapper
                    composable(Screen.Main.route) {
                        MainWithBarsScreen(
                            rootNavController = rootNavController,
                            startInChat = shouldNavigate,
                            notificationOtherUser = notificationOtherUser
                        )
                    }
                }
            }
        }
    }

    private fun handleNotificationTap() {
        intent?.let { intent ->
            if (intent.getBooleanExtra("clearNotification", false)) {
                val notificationSenderId = intent.getStringExtra("notificationSenderId")
                notificationSenderId?.let { senderId ->
                    // Clear the conversation and notification
                    NotificationReplyReceiver.clearConversationAndNotification(this, senderId)
                    Log.d("MainActivity", "Cleared notification for sender: $senderId")
                }
            }
        }
    }
}