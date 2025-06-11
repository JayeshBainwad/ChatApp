package com.jsb.chatapp.feature_auth.data.auth_repository

import com.jsb.chatapp.util.Result
import com.jsb.chatapp.feature_auth.domain.model.User

interface AuthRepository {
    suspend fun signup(email: String, password: String, username: String): Result<User>
    suspend fun signin(email: String, password: String): Result<User>
    suspend fun isUsernameAvailable(username: String): Boolean
    suspend fun getUserById(uid: String): Result<User>
    suspend fun updateUserProfile(uid: String, user: User): Result<Unit>
}