package com.jsb.chatapp.feature_main.main_navigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen
import com.jsb.chatapp.feature_main.main_domain.main_usecase.GetCurrentUserUseCase
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.MainWithBarsScreen
import com.jsb.chatapp.feature_main.theme.ChatAppTheme
import com.jsb.chatapp.feature_main.main_util.AppLifecycleObserver
import com.jsb.chatapp.feature_main.main_util.RequestPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val shouldNavigate = intent?.getBooleanExtra("navigateToChat", false) ?: false
                val senderId = intent?.getStringExtra("senderId")
                val senderName = intent?.getStringExtra("senderName")
                val senderPhoneNumber = intent?.getStringExtra("senderPhoneNumber")
                val senderAvatarUrl = intent?.getStringExtra("senderAvatarUrl")

                // State to hold the notification user data
                val notificationOtherUser = remember {
                    if (shouldNavigate && senderId != null && senderName != null && senderAvatarUrl != null) {
                        User(
                            uid = senderId,
                            username = senderName,
                            phoneNumber = senderPhoneNumber ?: "",
                            avatarUrl = senderAvatarUrl
                        )
                    } else null
                }

                // ✅ Handle notification click navigation
                LaunchedEffect(key1 = shouldNavigate, block = {
                    if (shouldNavigate && notificationOtherUser != null) {
                        // Check if user is authenticated
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUserId != null) {
                            // Navigate to main screen with notification data
                            rootNavController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
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
}