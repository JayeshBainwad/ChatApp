package com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.jsb.chatapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    onSignOut: () -> Unit,
    onHelp: () -> Unit
) {
    TopAppBar(
        title = { Text(
            text = title,
            fontSize = 26.sp
        ) },
        actions = {
            var expanded by remember { mutableStateOf(false) }
            IconButton(
                onClick = { expanded = true }
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
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
                        Icon(painterResource(R.drawable.signout) , contentDescription = "Help")
                    }
                )
            }
        }
    )
}
