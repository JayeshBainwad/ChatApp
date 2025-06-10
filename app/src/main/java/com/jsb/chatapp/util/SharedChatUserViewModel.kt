package com.jsb.chatapp.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.jsb.chatapp.feature_auth.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedChatUserViewModel @Inject constructor() : ViewModel() {
    var currentUser: User? by mutableStateOf(null)
    var otherUser: User? by mutableStateOf(null)

    fun setUsers(current: User, other: User) {
        currentUser = current
        otherUser = other
    }
}
