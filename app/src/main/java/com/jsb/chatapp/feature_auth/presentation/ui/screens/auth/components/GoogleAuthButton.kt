package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsb.chatapp.R
import com.jsb.chatapp.theme.ChatAppTheme

@Composable
fun GoogleAuthButton(
    onClick : () -> Unit,
    value : String,
    modifier : Modifier = Modifier,
    enabled : Boolean = true
){

    Card (
        onClick = onClick,
        modifier = modifier
            .clip(shape = RoundedCornerShape(percent = 40))
            .background(color = Color.Transparent)
            .fillMaxWidth(),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .background(color = Color.Transparent)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google Logo",
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@PreviewLightDark
@Composable
fun GoogleAuthButtonPreview(){
    ChatAppTheme {
        GoogleAuthButton(
            onClick = {},
            value = "Continue with Google",
            modifier = Modifier.fillMaxWidth()
        )
    }
}