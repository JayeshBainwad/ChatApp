package com.jsb.chatapp.feature_chat.domain.usecase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SearchUserRealtimeUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    operator fun invoke(query: String): Flow<List<User>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val userListeners = mutableMapOf<String, ListenerRegistration>()
        val currentUsers = mutableMapOf<String, User>()

        fun updateAndEmitUsers() {
            val sortedUsers = currentUsers.values.sortedBy { it.username }
            trySend(sortedUsers)
        }

        // First, search for users matching the query
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val searchSnapshot = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", query)
                    .whereLessThan("username", query + '\uf8ff')
                    .get()
                    .await()

                searchSnapshot.documents.forEach { document ->
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        currentUsers[user.uid] = user

                        // Set up real-time listener for this user
                        val userListener = firestore.collection("users")
                            .document(user.uid)
                            .addSnapshotListener { userDoc, error ->
                                if (error != null) {
                                    Log.e(
                                        "SearchUserRealtimeUseCase",
                                        "Error listening to user " +
                                                user.uid, error
                                    )
                                    return@addSnapshotListener
                                }

                                if (userDoc != null && userDoc.exists()) {
                                    val updatedUser = userDoc.toObject(User::class.java)
                                    if (updatedUser != null) {
                                        currentUsers[updatedUser.uid] = updatedUser
                                        updateAndEmitUsers()

                                        Log.d("SearchUserRealtimeUseCase",
                                            "Search user status updated: " +
                                                    "${updatedUser.username}, " +
                                                    "isOnline: ${updatedUser.isOnline}"
                                        )
                                    }
                                } else {
                                    // User was deleted, remove from current users
                                    currentUsers.remove(user.uid)
                                    updateAndEmitUsers()
                                }
                            }

                        userListeners[user.uid] = userListener
                    }
                }

                updateAndEmitUsers()

            } catch (e: Exception) {
                Log.e("SearchUserRealtimeUseCase", "Error searching users", e)
                close(e)
            }
        }

        awaitClose {
            userListeners.values.forEach { it.remove() }
        }
    }
}