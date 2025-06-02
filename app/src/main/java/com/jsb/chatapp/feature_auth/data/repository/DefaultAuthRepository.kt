package com.jsb.chatapp.feature_auth.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.core.util.Result
import com.jsb.chatapp.feature_auth.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DefaultAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override suspend fun signup(email: String, password: String, username: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.let {
                val newUser = User(uid = it.uid, email = email, username = username, createdAt = System.currentTimeMillis())
                firestore.collection("users").document(it.uid).set(newUser).await()
                newUser
            }
            if (user != null) Result.Success(user) else Result.Error(Exception("Signup failed"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.let {
                firestore.collection("users").document(it.uid).get().await()
                    .toObject(User::class.java)?.copy(uid = it.uid)
            }
            if (user != null) Result.Success(user) else Result.Error(Exception("User not found"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}