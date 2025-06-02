package com.jsb.chatapp.feature_auth.data.datasource

import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.core.util.Result

interface AuthDataSource {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signup(email: String, password: String, username: String): Result<User>
}