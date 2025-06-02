package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import com.jsb.chatapp.feature_auth.domain.model.User


data class SignInResult(
    val data: User?,
    val errorMessage: String?
)