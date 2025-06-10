package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jsb.chatapp.R
import com.jsb.chatapp.feature_auth.domain.model.User

@Composable
fun CustomUserCard(
    otherUserId: String,
    otherUserName: String,
    otherUserAvatar: String,
    otherUserPhoneNumber: String?,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val imageModel = otherUserAvatar.ifBlank {
        R.drawable.placeholder_profile
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable {
                onClick()
            },
        border = BorderStroke(width = 1.5.dp, color = colors.onSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                placeholder = painterResource(R.drawable.placeholder_profile),
                error = painterResource(R.drawable.placeholder_profile)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(text = otherUserName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                otherUserPhoneNumber?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}