package com.jsb.chatapp.feature_auth.domain.usecase

import com.jsb.chatapp.util.Result
import com.jsb.chatapp.feature_auth.data.repository.AuthRepository
import com.jsb.chatapp.feature_auth.domain.model.User
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, username: String): Result<User> {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            return Result.Error(Exception("Fields cannot be empty"))
        }
        if (password.length < 6) {
            return Result.Error(Exception("Password must be at least 6 characters"))
        }
        return repository.signup(email, password, username)
    }
}