package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jsb.chatapp.Screen
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.components.AuthButton
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.components.AuthTextField
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.components.GoogleAuthButton
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.components.OrSignInWithDivider
import com.jsb.chatapp.theme.ChatAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SignInScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                viewModel.signInWithGoogleIntent(intent)
            }
        }
    }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            delay(1000)
            navController.navigate(Screen.ChatHome.route)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiEvent = viewModel.uiEvent

    LaunchedEffect(key1 = true) {
        uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome back!",
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome to ChatApp - your space to connect, share, and stay in touch with the people who matter most.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(AuthEvent.UpdateEmail(it)) },
                label = "Email"
            )

            Spacer(modifier = Modifier.height(10.dp))

            AuthTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(AuthEvent.UpdatePassword(it)) },
                label = "Password",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = { viewModel.onEvent(AuthEvent.ToggleRememberMe(it)) }
                    )
                    Text(
                        text = "Remember me",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    )
                }

                TextButton(onClick = { viewModel.onEvent(AuthEvent.ForgotPassword) }) {
                    Text(
                        text = "Forgot Password?",
                        fontSize = 14.sp
                    )
                }
            }


            Spacer(modifier = Modifier.height(8.dp))

            AuthButton(
                onClick = { viewModel.onEvent(AuthEvent.Signin) },
                value = "Sign in",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            OrSignInWithDivider(
                text = "Or sign in with"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Google Sign-In Button
            GoogleAuthButton(
                onClick = {
                    viewModel.viewModelScope.launch {
                        val intentSender = viewModel.launchGoogleSignIn()
                        intentSender?.let {
                            launcher.launch(
                                IntentSenderRequest.Builder(it).build()
                            )
                        }
                    }
                },
                value = "Continue with Google",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate(Screen.Signup.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? Sign up")
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    ChatAppTheme {
        SignInScreen(rememberNavController())
    }
}