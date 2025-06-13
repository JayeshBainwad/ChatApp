package com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_auth.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    onSignOut: () -> Unit,
    onHelp: () -> Unit,
    // Chat screen specific parameters
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    otherUser: User? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (showBackButton) 22.sp else 26.sp,
                modifier = if (showBackButton) Modifier.padding(start = 16.dp) else Modifier
            )
        },
        navigationIcon = {
            if (showBackButton && onBackClick != null && otherUser != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onBackClick() }
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    AsyncImage(
                        model = otherUser.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        placeholder = painterResource(R.drawable.placeholder_profile),
                        error = painterResource(R.drawable.placeholder_profile)
                    )
                }
            }
        },
        actions = {
            // Only show menu for main screens, not for chat screen
            if (!showBackButton) {
                var expanded by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { expanded = true }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(22.dp))
                        .background(color = MaterialTheme.colorScheme.surfaceContainer),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Help") },
                        onClick = {
                            expanded = false
                            onHelp()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.help), contentDescription = "Help")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sign out") },
                        onClick = {
                            expanded = false
                            onSignOut()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.signout), contentDescription = "Sign out")
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}