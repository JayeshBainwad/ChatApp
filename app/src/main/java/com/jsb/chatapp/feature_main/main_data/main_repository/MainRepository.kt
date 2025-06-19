package com.jsb.chatapp.main_data.main_repository

import com.jsb.chatapp.main_domain.main_model.User
import com.jsb.chatapp.util.Result

interface MainRepository {
    suspend fun getUserById(uid: String): Result<User>
}