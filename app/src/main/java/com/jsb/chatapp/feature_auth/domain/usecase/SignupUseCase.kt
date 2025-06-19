package com.jsb.chatapp.feature_auth.domain.usecase

import com.jsb.chatapp.feature_main.main_util.Result
import com.jsb.chatapp.feature_auth.data.auth_repository.AuthRepository
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_chat.domain.usecase.IsUsernameAvailableUseCase
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val isUsernameAvailableUseCase: IsUsernameAvailableUseCase
) {
    suspend operator fun invoke(email: String, password: String, username: String): Result<User> {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            return Result.Error(Exception("Fields cannot be empty"))
        }
        if (password.length < 6) {
            return Result.Error(Exception("Password must be at least 6 characters"))
        }
        if (username.length < 3) {
            return Result.Error(Exception("Username must be at least 3 characters"))
        }

        val isAvailable = isUsernameAvailableUseCase(username)
        if (!isAvailable) {
            return Result.Error(Exception("'$username' is already taken"))
        }

        return repository.signup(email, password, username)
    }
}