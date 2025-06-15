package com.jsb.chatapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignInScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.SignupScreen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.splash.SplashScreen
import com.jsb.chatapp.feature_chat.domain.usecase.GetCurrentUserUseCase
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.MainWithBarsScreen
import com.jsb.chatapp.theme.ChatAppTheme
import com.jsb.chatapp.util.RequestPermission
import com.jsb.chatapp.util.SharedChatUserViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.jsb.chatapp.util.Result


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    @SuppressLint("UnrememberedGetBackStackEntry")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
         WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        setContent {



            ChatAppTheme {
                RequestPermission()
                val rootNavController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val sharedUserViewModel: SharedChatUserViewModel = hiltViewModel()

                // ✅ Extract intent extras
                val shouldNavigate = intent?.getBooleanExtra("navigateToChat", false) ?: false
                val senderId = intent?.getStringExtra("senderId")
                val senderName = intent?.getStringExtra("senderName")
                val senderFcmToken = intent?.getStringExtra("senderFcmToken")

                // ✅ Handle notification click navigation
                LaunchedEffect(key1 = shouldNavigate, block = {
                    if (shouldNavigate && senderId != null && senderName != null && senderFcmToken != null) {
                        val currentResult = getCurrentUserUseCase(FirebaseAuth.getInstance().uid!!)
                        val otherResult = getCurrentUserUseCase(senderId)

                        if (currentResult is Result.Success && otherResult is Result.Success) {
                            sharedUserViewModel.setUsers(
                                current = currentResult.data,
                                other = otherResult.data
                            )
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
                        val sharedUserViewModel1: SharedChatUserViewModel = hiltViewModel()
                        MainWithBarsScreen(
                            rootNavController = rootNavController,
                            sharedUserViewModel = sharedUserViewModel1,
                            startInChat = shouldNavigate
                        )
                    }
                }
            }
        }
    }
}