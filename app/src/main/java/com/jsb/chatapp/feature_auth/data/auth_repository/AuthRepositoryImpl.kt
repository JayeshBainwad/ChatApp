package com.jsb.chatapp.feature_auth.data.auth_repository

import com.jsb.chatapp.feature_auth.data.auth_datasource.AuthDataSource
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource
) : AuthRepository {
    override suspend fun signin(email: String, password: String) =
        authDataSource.signin(email, password)

    override suspend fun signup(email: String, password: String, username: String) =
        authDataSource.signup(email, password, username)




}