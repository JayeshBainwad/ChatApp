package com.jsb.chatapp.feature_auth.data.auth_datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.jsb.chatapp.feature_main.main_util.Result
import android.util.Log

class AuthDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthDataSource {

    override suspend fun signin(email: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user?.let { firebaseUser ->
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val existingUser = userDoc.toObject(User::class.java)?.copy(uid = firebaseUser.uid)

            // Update FCM token and online status on signin
            if (existingUser != null) {
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.e("AuthDataSourceImpl", "Failed to get FCM token on signin", e)
                    existingUser.fcmToken // Keep existing token if new one fails
                }

                val updatedUser = existingUser.copy(
                    fcmToken = fcmToken,
                    isOnline = true,
                    lastSeen = System.currentTimeMillis()
                )

                val updated = mapOf(
                    "fcmToken" to fcmToken,
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                )

                // Update in Firestore
                firestore.collection("users").document(firebaseUser.uid).update(updated).await()
                updatedUser
            } else {
                existingUser
            }
        }
        if (user != null) Result.Success(user) else Result.Error(Exception("User not found"))
    } catch (e: Exception) {
        Log.e("AuthDataSourceImpl", "Login error", e)
        Result.Error(e)
    }

    override suspend fun signup(email: String, password: String, username: String): Result<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user?.let { firebaseUser ->
            // Get FCM token for new user
            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                Log.e("AuthDataSourceImpl", "Failed to get FCM token on signup", e)
                "" // Default empty if token generation fails
            }

            val currentTime = System.currentTimeMillis()

            // Create explicit map to avoid field name conflicts
            val userData = mapOf(
                "uid" to firebaseUser.uid,
                "username" to username,
                "name" to (firebaseUser.displayName ?: ""), // Use display name from Firebase if available
                "email" to email,
                "avatarUrl" to (firebaseUser.photoUrl?.toString() ?: ""), // Use photo URL if available
                "phoneNumber" to firebaseUser.phoneNumber,
                "bio" to "", // Default empty bio
                "fcmToken" to fcmToken,
                "lastSeen" to currentTime,
                "createdAt" to currentTime,
                "isOnline" to true // User is online when they just signed up - explicitly use "isOnline"
            )

            // Use map instead of data class to prevent unwanted field creation
            firestore.collection("users").document(firebaseUser.uid).set(userData).await()
            Log.d("AuthDataSourceImpl", "New user created with FCM token: $fcmToken")

            // Return User object for the result
            User(
                uid = firebaseUser.uid,
                username = username,
                name = firebaseUser.displayName ?: "",
                email = email,
                avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                phoneNumber = firebaseUser.phoneNumber,
                bio = "",
                fcmToken = fcmToken,
                lastSeen = currentTime,
                createdAt = currentTime,
                isOnline = true
            )
        }
        if (user != null) Result.Success(user) else Result.Error(Exception("Signup failed"))
    } catch (e: Exception) {
        Log.e("AuthDataSourceImpl", "Signup error", e)
        Result.Error(e)
    }
}