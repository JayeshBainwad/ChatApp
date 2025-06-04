package com.jsb.chatapp.feature_auth.domain.usecase

import com.jsb.chatapp.util.Result
import com.jsb.chatapp.feature_auth.data.repository.AuthRepository
import com.jsb.chatapp.feature_auth.domain.model.User
import javax.inject.Inject

class SigninUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) {
            return Result.Error(Exception("Email cannot be empty"))
        }
        if (password.isBlank()) {
            return Result.Error(Exception("Password cannot be empty"))
        }
        return repository.signin(email, password)
    }
}