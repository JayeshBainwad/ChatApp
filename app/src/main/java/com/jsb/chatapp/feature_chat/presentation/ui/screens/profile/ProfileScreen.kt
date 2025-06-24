package com.jsb.chatapp.feature_chat.presentation.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val uiEvent = viewModel.uiEvent
    val snackbarHostState = remember { SnackbarHostState() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.onEvent(ProfileEvent.OnAvatarSelected(it))
        }
    }


    LaunchedEffect(Unit) {
        viewModel.onEvent(ProfileEvent.LoadProfile)
    }

    LaunchedEffect(true) {
        uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = state.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(width = 0.6.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                    .clickable {
                        launcher.launch("image/*") // Open image picker
                    },
                placeholder = painterResource(id = R.drawable.placeholder_profile),
                error = painterResource(id = R.drawable.placeholder_profile)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onEvent(ProfileEvent.OnUsernameChanged(it)) },
                label = { Text("Username") },
                singleLine = true,
                isError = state.isUsernameAvailable == false
            )
            if (state.isUsernameAvailable == false) {
                Text(
                    text = "${state.username} already taken",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(ProfileEvent.OnNameChanged(it)) },
                label = { Text("Full Name") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = { viewModel.onEvent(ProfileEvent.OnPhoneChanged(it)) },
                label = { Text("Phone Number") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = {},
                label = { Text("Email (read-only)") },
                singleLine = true,
                readOnly = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.onEvent(ProfileEvent.UpdateProfile) }) {
                Text("Save")
            }
        }
    }
}
