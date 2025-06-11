package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_auth.data.auth_repository.AuthRepository
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.util.Result
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(uid: String): Result<User> {
        return repository.getUserById(uid)
    }
}