package com.jsb.chatapp.feature_main.main_data.main_repository

import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_main.main_util.Result

interface MainRepository {
    suspend fun getUserById(uid: String): Result<User>
}