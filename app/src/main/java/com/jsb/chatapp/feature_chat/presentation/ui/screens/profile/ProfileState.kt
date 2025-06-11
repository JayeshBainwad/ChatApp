package com.jsb.chatapp.feature_chat.presentation.ui.screens.profile

data class ProfileState(
    val email: String = "",
    val username: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val isUsernameAvailable: Boolean? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)
