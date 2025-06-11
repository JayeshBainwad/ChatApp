package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_auth.data.auth_repository.AuthRepository
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.util.Result
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(uid: String, user: User): Result<Unit> {
        return repository.updateUserProfile(uid, user)
    }
}