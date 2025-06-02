package com.jsb.chatapp.feature_auth.data.repository

import com.jsb.chatapp.core.util.Result
import com.jsb.chatapp.feature_auth.domain.model.User

interface AuthRepository {
    suspend fun signup(email: String, password: String, username: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
}