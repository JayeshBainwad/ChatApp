package com.jsb.chatapp.feature_chat.presentation.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.AuthViewModel
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatHomeScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatHomeViewModel
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components.BottomNavigationBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components.CustomTopAppBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.profile.ProfileScreen
import com.jsb.chatapp.feature_news.presentation.ui.screens.NewsScreen
import com.jsb.chatapp.util.SharedChatUserViewModel

@Composable
fun MainWithBarsScreen(
    rootNavController: NavHostController,
    sharedUserViewModel: SharedChatUserViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    chatHomeViewModel: ChatHomeViewModel = hiltViewModel(),
    startInChat: Boolean = false
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Collect auth state to handle sign out navigation
    val authState by authViewModel.state.collectAsState()
    val firebaseUser by authViewModel.currentUser.collectAsState()
    val currentUser = sharedUserViewModel.currentUser
    val otherUser = sharedUserViewModel.otherUser

    // State for sign out confirmation dialog
    var showSignOutDialog by remember { mutableStateOf(false) }

    val title = when (currentRoute) {
        Screen.ChatHome.route -> "Chat App"
        Screen.Profile.route -> "Profile"
        Screen.News.route -> "News"
        Screen.Chat.route -> otherUser?.username ?: "Chat"
        else -> ""
    }

    val showBackButton = currentRoute == Screen.Chat.route
    val showBottomBar = currentRoute != Screen.Chat.route

    LaunchedEffect(startInChat, currentUser, otherUser) {
        if (startInChat && currentUser != null && otherUser != null) {
            mainNavController.navigate(Screen.Chat.route)
        }
    }

    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        // Use ChatHomeViewModel's logout function instead of AuthViewModel's signOut
                        chatHomeViewModel.logout {
                            // Navigation will be handled by the LaunchedEffect above
                            // when auth state changes
                            android.util.Log.d("MainWithBarsScreen", "User signed out, navigating to auth")
                            rootNavController.navigate(Screen.Signin.route) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = title,
                onSignOut = {
                    showSignOutDialog = true
                },
                onHelp = { /* TODO */ },
                showBackButton = showBackButton,
                onBackClick = if (showBackButton) {
                    { mainNavController.navigateUp() }
                } else null,
                otherUser = otherUser
            )
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        mainNavController.navigate(route) {
                            // Fix: popUpTo must refer to a destination inside mainNavController's graph
                            popUpTo(mainNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = mainNavController,
                startDestination = Screen.ChatHome.route
            ) {
                composable(Screen.ChatHome.route) {
                    ChatHomeScreen(
                        rootNavController = rootNavController,
                        sharedUserViewModel = sharedUserViewModel,
                        mainNavController = mainNavController
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(rootNavController)
                }
                composable(Screen.News.route) {
                    NewsScreen()
                }
                composable(Screen.Chat.route) {
                    // Get users from shared view model
                    currentUser?.let { current ->
                        otherUser?.let { other ->
                            val chatId = listOf(current.uid, other.uid).sorted().joinToString("_")
                            ChatScreen(
                                chatId = chatId,
                                currentUser = current,
                                otherUser = other,
                                navController = mainNavController
                            )
                        }
                    } ?: run {
                        // Show loading or error state if users are not available
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Loading chat... Please wait.")
                        }
                    }
                }
            }
        }
    }
}