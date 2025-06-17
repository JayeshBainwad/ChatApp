package com.jsb.chatapp.feature_auth.data.auth_datasource

import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.util.Result
import com.google.firebase.auth.FirebaseUser

interface AuthDataSource {
    suspend fun signin(email: String, password: String): Result<User>
    suspend fun signup(email: String, password: String, username: String): Result<User>
//    suspend fun googleSignin(firebaseUser: FirebaseUser): Result<User>
//    suspend fun googleSignup(firebaseUser: FirebaseUser, username: String): Result<User>
    suspend fun isUsernameAvailable(username: String): Boolean
    suspend fun getUserById(uid: String): Result<User>
    suspend fun updateUserProfile(uid: String, user: User): Result<Unit>
}