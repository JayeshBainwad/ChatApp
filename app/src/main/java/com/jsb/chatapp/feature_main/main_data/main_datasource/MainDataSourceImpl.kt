package com.jsb.chatapp.feature_main.main_data.main_datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_main.main_util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MainDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): MainDataSource {
    override suspend fun getUserById(uid: String): Result<User> = try {
        val doc = firestore.collection("users").document(uid).get().await()
        val user = doc.toObject(User::class.java)
        if (user != null) Result.Success(user) else Result.Error(Exception("User not found"))
    } catch (e: Exception) {
        Result.Error(e)
    }
}