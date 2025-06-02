package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

data class AuthState(
    val email: String = "",
    val password: String = "",
    val username: String = "", // For Signup
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)