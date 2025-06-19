package com.jsb.chatapp.feature_auth.data.auth_datasource

import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_core.main_util.Result

interface AuthDataSource {
    suspend fun signin(email: String, password: String): Result<User>
    suspend fun signup(email: String, password: String, username: String): Result<User>
}