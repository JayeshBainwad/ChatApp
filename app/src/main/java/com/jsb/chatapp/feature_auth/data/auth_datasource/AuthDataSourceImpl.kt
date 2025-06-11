package com.jsb.chatapp.feature_auth.data.auth_datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_auth.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.jsb.chatapp.util.Result
import android.util.Log

class AuthDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthDataSource {

    override suspend fun signin(email: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user?.let {
            firestore.collection("users").document(it.uid).get().await()
                .toObject(User::class.java)?.copy(uid = it.uid)
        }
        if (user != null) Result.Success(user) else Result.Error(Exception("User not found"))
    } catch (e: Exception) {
        Log.e("AuthDataSourceImpl", "Login error", e)
        Result.Error(e)
    }

    override suspend fun signup(email: String, password: String, username: String): Result<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user?.let {
            val newUser = User(
                uid = it.uid,
                username = username,
                name = "", // Optional: or collect a real name in UI
                email = email,
                avatarUrl = "",
                phoneNumber = it.phoneNumber,
                bio = "",
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(it.uid).set(newUser).await()
            newUser
        }
        if (user != null) Result.Success(user) else Result.Error(Exception("Signup failed"))
    } catch (e: Exception) {
        Log.e("AuthDataSourceImpl", "Signup error", e)
        Result.Error(e)
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        val snapshot = firestore
            .collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        return snapshot.isEmpty
    }

    override suspend fun getUserById(uid: String): Result<User> = try {
        val doc = firestore.collection("users").document(uid).get().await()
        val user = doc.toObject(User::class.java)
        if (user != null) Result.Success(user) else Result.Error(Exception("User not found"))
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun updateUserProfile(uid: String, user: User): Result<Unit> = try {
        firestore.collection("users").document(uid).set(user).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
