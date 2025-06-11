package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_auth.data.auth_repository.AuthRepository
import javax.inject.Inject

class IsUsernameAvailableUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String): Boolean {
        return repository.isUsernameAvailable(username)
    }
}