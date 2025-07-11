package com.jsb.chatapp.feature_chat.presentation.ui.screens.main

import com.jsb.chatapp.feature_core.core_domain.main_model.User

sealed class MainScreenEvent {
    object ShowSignOutDialog : MainScreenEvent()
    object HideSignOutDialog : MainScreenEvent()
    object ConfirmSignOut : MainScreenEvent()
    data class UpdateCurrentRoute(val route: String?) : MainScreenEvent()
    data class SelectChatUser(val userId: String) : MainScreenEvent()
    data class SetChatUsers(
        val otherUser: User
    ) : MainScreenEvent()
    object ClearSelectedChat : MainScreenEvent()
}