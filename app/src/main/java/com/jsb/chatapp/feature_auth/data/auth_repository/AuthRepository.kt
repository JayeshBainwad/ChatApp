package com.jsb.chatapp.feature_auth.data.auth_repository

import com.jsb.chatapp.feature_core.main_util.Result
import com.jsb.chatapp.feature_core.core_domain.main_model.User

interface AuthRepository {
    suspend fun signup(email: String, password: String, username: String): Result<User>
    suspend fun signin(email: String, password: String): Result<User>



}