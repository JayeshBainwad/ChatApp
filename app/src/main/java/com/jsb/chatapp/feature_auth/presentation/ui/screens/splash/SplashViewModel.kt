package com.jsb.chatapp.feature_auth.presentation.ui.screens.splash

import android.annotation.SuppressLint
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import javax.inject.Inject
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
): ViewModel() {
    private val _firestoreUser = mutableStateOf<User?>(null)
    val firestoreUser: State<User?> = _firestoreUser

    init {
        fetchCurrentUserFromFirestore()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun fetchCurrentUserFromFirestore() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _firestoreUser.value = doc.toObject(User::class.java)
                } else {
                    Log.w("ChatViewModel", "User doc not found for uid=$uid")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error fetching user doc", e)
            }
    }
}