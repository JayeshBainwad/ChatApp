package com.jsb.chatapp.feature_core.core_data.main_repository

import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_core.main_util.Result

interface MainRepository {
    suspend fun getUserById(uid: String): Result<User>
}