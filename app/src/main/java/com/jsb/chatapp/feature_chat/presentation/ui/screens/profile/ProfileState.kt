package com.jsb.chatapp.feature_chat.presentation.ui.screens.profile

data class ProfileState(
    val email: String = "",
    val username: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val bio: String = "", // Add bio field
    val avatarUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUsernameAvailable: Boolean? = null,
    val isSaved: Boolean = false
)
