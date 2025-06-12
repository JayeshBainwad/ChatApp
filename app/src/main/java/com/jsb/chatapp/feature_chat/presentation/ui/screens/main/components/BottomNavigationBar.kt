package com.jsb.chatapp.feature_chat.presentation.ui.screens.main.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        listOf(
            BottomNavItem.ChatHome,
            BottomNavItem.Profile,
            BottomNavItem.News
        ).forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                icon = { Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = item.label,
                    modifier = Modifier.size(34.dp)
                ) },
                label = {
                    Text(
                        item.label,
                        fontSize = 14.sp
                ) }
            )
        }
    }
}
