package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jsb.chatapp.R

@Composable
fun CustomUserCard(
    otherUserName: String,
    otherUserAvatar: String,
    otherUserLastMessage: String?,
    lastMessageTime: String? = null,
    onClick: () -> Unit
) {
    val imageModel = otherUserAvatar.ifBlank {
        R.drawable.placeholder_profile
    }

    Card(
        modifier = Modifier
            .background(color = Color.Transparent)
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Optional: remove shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Transparent)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .background(color = Color.Transparent)
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(width = 0.6.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape),
                placeholder = painterResource(R.drawable.placeholder_profile),
                error = painterResource(R.drawable.placeholder_profile)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .background(color = Color.Transparent)
            ) {
                Text(text = otherUserName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                otherUserLastMessage?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // TODO: Last message time and show number of unread messages
                if (!lastMessageTime.isNullOrBlank()) {
                    Text(
                        text = lastMessageTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

            }
        }
    }
}