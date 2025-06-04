package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.components


import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsb.chatapp.R
import com.jsb.chatapp.theme.ChatAppTheme

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .background(color = Color.Transparent)
                .clip(shape = RoundedCornerShape(percent = 40))
                .fillMaxWidth(),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    val iconPainter = if (passwordVisible) {
                        painterResource(id = R.drawable.visibility_on)
                    } else {
                        painterResource(id = R.drawable.visibility_off)
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else trailingIcon,
            isError = isError,
            singleLine = true
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                fontSize = 12.sp
            )
        }
    }
}

@PreviewLightDark
@Composable
fun AuthTextFieldPreview(){
    ChatAppTheme {
        AuthTextField(
            value = "Hello",
            onValueChange = {},
            label = "Email",
            modifier = Modifier.fillMaxWidth()
        )
    }
}