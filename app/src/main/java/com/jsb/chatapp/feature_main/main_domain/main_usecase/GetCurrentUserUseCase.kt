package com.jsb.chatapp.feature_main.main_domain.main_usecase

import com.jsb.chatapp.feature_main.main_data.main_repository.MainRepository
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_main.main_util.Result
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: MainRepository
) {
    suspend operator fun invoke(uid: String): Result<User> {
        return repository.getUserById(uid)
    }
}