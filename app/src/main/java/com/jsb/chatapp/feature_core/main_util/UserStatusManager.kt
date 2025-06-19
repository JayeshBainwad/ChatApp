package com.jsb.chatapp.feature_core.main_util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatusManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "UserStatusManager"
    }

    private suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            val updates = mutableMapOf<String, Any>(
                "isOnline" to isOnline
            )

            // Update lastSeen only when going offline
            if (!isOnline) {
                updates["lastSeen"] = System.currentTimeMillis()
            }

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Log.d(TAG, "Updated user $userId online status to: $isOnline")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user online status", e)
        }
    }

    suspend fun setUserOnline(userId: String) {
        updateUserOnlineStatus(userId, true)
    }

    suspend fun setUserOffline(userId: String) {
        updateUserOnlineStatus(userId, false)
    }
}