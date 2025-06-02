package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

sealed class AuthEvent {
    data class UpdateEmail(val email: String) : AuthEvent()
    data class UpdatePassword(val password: String) : AuthEvent()
    data class UpdateUsername(val username: String) : AuthEvent()
    object Signin : AuthEvent()
    object Signup : AuthEvent()
}
