package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}
