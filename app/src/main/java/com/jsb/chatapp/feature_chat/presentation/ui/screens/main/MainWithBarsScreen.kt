package com.jsb.chatapp.feature_chat.presentation.ui.screens.main

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.feature_core.core_navigation.Screen
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.ChatScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatHomeScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatHomeViewModel
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components.BottomNavigationBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components.CustomTopAppBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.profile.ProfileScreen
import com.jsb.chatapp.feature_news.presentation.ui.screens.NewsScreen

@Composable
fun MainWithBarsScreen(
    rootNavController: NavHostController,
    chatHomeViewModel: ChatHomeViewModel = hiltViewModel(),
    mainScreenViewModel: MainScreenViewModel = hiltViewModel(),
    startInChat: Boolean = false,
    notificationOtherUser: User? = null // For notification navigation
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Collect states
    val mainScreenState by mainScreenViewModel.state.collectAsState()

    // Update current route in view model
    LaunchedEffect(currentRoute) {
        mainScreenViewModel.onEvent(MainScreenEvent.UpdateCurrentRoute(currentRoute))
    }

    // Handle navigation to chat when startInChat is true (from notification)
    LaunchedEffect(startInChat, notificationOtherUser, mainScreenState.currentUser) {
        if (startInChat && notificationOtherUser != null && mainScreenState.currentUser != null) {
            Log.d("FCM_DEBUG", "MainWithBarsScreen - Setting chat users and navigating")

            mainScreenViewModel.onEvent(
                MainScreenEvent.SetChatUsers(
                    otherUser = notificationOtherUser
                )
            )

            mainNavController.navigate(Screen.Chat.route) {
                popUpTo(Screen.ChatHome.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Clear selected chat when navigating away from chat
    // Update the clearing LaunchedEffect:
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute != Screen.Chat.route) {
            Log.d("FCM_DEBUG", "MainWithBarsScreen - Clearing selected chat, current route: $currentRoute")
            mainScreenViewModel.onEvent(MainScreenEvent.ClearSelectedChat)
        }
    }

    // Wait for current user to be loaded before proceeding
    LaunchedEffect(mainScreenState.currentUser) {
        if (mainScreenState.currentUser != null) {
            Log.d("FCM_DEBUG", "MainWithBarsScreen - Current user loaded: ${mainScreenState.currentUser?.username}")
        }
    }

    val title = mainScreenViewModel.getTitle(currentRoute)
    val showBackButton = mainScreenViewModel.shouldShowBackButton(currentRoute)
    val showBottomBar = mainScreenViewModel.shouldShowBottomBar(currentRoute)

    // Sign out confirmation dialog
    if (mainScreenState.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = {
                mainScreenViewModel.onEvent(MainScreenEvent.HideSignOutDialog)
            },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainScreenViewModel.onEvent(MainScreenEvent.ConfirmSignOut)
                        // Use ChatHomeViewModel's logout function
                        chatHomeViewModel.logout {
                            Log.d("MainWithBarsScreen", "User signed out, navigating to auth")
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
                    onClick = {
                        mainScreenViewModel.onEvent(MainScreenEvent.HideSignOutDialog)
                    }
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
                    mainScreenViewModel.onEvent(MainScreenEvent.ShowSignOutDialog)
                },
                onHelp = { /* TODO */ },
                showBackButton = showBackButton,
                onBackClick = if (showBackButton) {
                    {
                        // Clear selected chat when going back
                        mainScreenViewModel.onEvent(MainScreenEvent.ClearSelectedChat)
                        mainNavController.navigateUp()
                    }
                } else null,
                otherUser = mainScreenState.selectedOtherUser // Pass the selected user from state
            )
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        mainNavController.navigate(route) {
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
                        mainNavController = mainNavController,
                        mainScreenViewModel = mainScreenViewModel
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen()
                }
                composable(Screen.News.route) {
                    NewsScreen()
                }
                composable(Screen.Chat.route) {
                    // Wait for both users to be available before showing ChatScreen
                    val currentUser = mainScreenState.currentUser
                    val otherUser = mainScreenState.selectedOtherUser

                    Log.d("FCM_DEBUG", "ChatScreen composable - currentUser: ${currentUser?.username}")
                    Log.d("FCM_DEBUG", "ChatScreen composable - otherUser: ${otherUser?.username}")
                    Log.d("FCM_DEBUG", "ChatScreen composable - otherUser FCM: ${otherUser?.fcmToken}")

                    if (currentUser != null && otherUser != null) {
                        ChatScreen(
                            currentUser = currentUser,
                            otherUser = otherUser
                        )
                    } else {
                        // Show loading state while users are being loaded
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