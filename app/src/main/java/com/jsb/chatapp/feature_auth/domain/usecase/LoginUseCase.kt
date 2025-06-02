package com.jsb.chatapp.feature_auth.domain.usecase

import com.jsb.chatapp.core.util.Result
import com.jsb.chatapp.feature_auth.data.repository.AuthRepository
import com.jsb.chatapp.feature_auth.domain.model.User
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(Exception("Email and password cannot be empty"))
        }
        return repository.login(email, password)
    }
}