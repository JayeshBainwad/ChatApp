package com.jsb.chatapp.feature_chat.presentation.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatHomeScreen
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components.BottomNavigationBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components.CustomTopAppBar
import com.jsb.chatapp.feature_chat.presentation.ui.screens.profile.ProfileScreen
import com.jsb.chatapp.feature_news.presentation.ui.screens.NewsScreen
import com.jsb.chatapp.util.SharedChatUserViewModel

@Composable
fun MainWithBarsScreen(
    rootNavController: NavHostController,
    sharedUserViewModel: SharedChatUserViewModel
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val title = when (currentRoute) {
        Screen.ChatHome.route -> "Chat App"
        Screen.Profile.route -> "Profile"
        Screen.News.route -> "News"
        else -> ""
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = title,
                onSignOut = {
                    rootNavController.navigate(Screen.Signin.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onHelp = { /* TODO */ }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onItemClick = { route ->
                    mainNavController.navigate(route)
                    {
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

    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = mainNavController,
                startDestination = Screen.ChatHome.route
            ) {
                composable(Screen.ChatHome.route) {
                    ChatHomeScreen(
                        rootNavController = rootNavController,
                        sharedUserViewModel = sharedUserViewModel
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(rootNavController)
                }
                composable(Screen.News.route) {
                    NewsScreen()
                }
            }
        }
    }
}


