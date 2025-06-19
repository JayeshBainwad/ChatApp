package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import com.jsb.chatapp.feature_core.core_domain.main_model.User


data class SignInResult(
    val data: User?,
    val errorMessage: String?
)