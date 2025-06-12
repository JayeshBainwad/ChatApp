package com.jsb.chatapp.feature_auth.presentation.ui.screens.splash

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jsb.chatapp.R
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.AuthViewModel
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val user by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val rememberMeFlow = remember { UserPreferences(context).rememberMeFlow }

    var rememberMe by remember { mutableStateOf(false) }

    val firestoreUser = viewModel.firestoreUser.value

    // Collect the Remember Me value from DataStore
    LaunchedEffect(Unit) {
        rememberMeFlow.collect { value ->
            rememberMe = value
            Log.d("SplashScreen", "RememberMe = $value")
        }
    }

    // Navigate based on user and Remember Me
    LaunchedEffect(user, rememberMe) {
        delay(1000L) // Optional delay for splash effect
        if (user != null && rememberMe) {
            Log.d("SplashNav", "User ${firestoreUser?.username} authenticated and RememberMe is true")

            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            Log.d("SplashNav", "Redirecting to SignIn: User=${user != null}, RememberMe=$rememberMe")

            navController.navigate(Screen.Signin.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .clip(shape = RoundedCornerShape(percent = 40))
                .size(180.dp)
        )
    }
}