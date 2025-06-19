package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_core.main_util.Result
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(uid: String, user: User): Result<Unit> {
        return repository.updateUserProfile(uid, user)
    }
}