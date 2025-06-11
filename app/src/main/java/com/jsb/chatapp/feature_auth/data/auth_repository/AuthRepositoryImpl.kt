package com.jsb.chatapp.feature_auth.data.auth_repository

import com.jsb.chatapp.feature_auth.data.auth_datasource.AuthDataSource
import com.jsb.chatapp.feature_auth.domain.model.User
import javax.inject.Inject
import com.jsb.chatapp.util.Result

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource
) : AuthRepository {
    override suspend fun signin(email: String, password: String) =
        authDataSource.signin(email, password)

    override suspend fun signup(email: String, password: String, username: String) =
        authDataSource.signup(email, password, username)

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return authDataSource.isUsernameAvailable(username)
    }

    override suspend fun getUserById(uid: String): Result<User> {
        return authDataSource.getUserById(uid)
    }

    override suspend fun updateUserProfile(uid: String, user: User): Result<Unit> {
        return authDataSource.updateUserProfile(uid, user)
    }

}