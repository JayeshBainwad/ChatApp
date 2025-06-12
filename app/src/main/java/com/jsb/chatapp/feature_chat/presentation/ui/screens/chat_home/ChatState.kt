package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.UserChatInfo

data class ChatState(
    val isLoading: Boolean = false,
    val userChatInfos: List<UserChatInfo> = emptyList(), // Combined user and chat info
    val error: String? = null,
    val query: String = "",
)
