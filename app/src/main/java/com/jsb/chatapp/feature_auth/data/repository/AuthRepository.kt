package com.jsb.chatapp.feature_auth.data.repository

import com.google.firebase.auth.FirebaseUser
import com.jsb.chatapp.util.Result
import com.jsb.chatapp.feature_auth.domain.model.User

interface AuthRepository {
    suspend fun signup(email: String, password: String, username: String): Result<User>
    suspend fun signin(email: String, password: String): Result<User>
    suspend fun isUsernameAvailable(username: String): Boolean
}