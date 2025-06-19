package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import com.jsb.chatapp.R

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = Firebase.auth

    suspend fun signIn(): IntentSender? {
        Log.d("GoogleAuthUiClient", "Starting Google Sign-In process")
        val result = try {
            val signInRequest = buildSignInRequest()
            val signInResult = oneTapClient.beginSignIn(signInRequest).await()
            Log.d("GoogleAuthUiClient", "Sign-In request initiated successfully")
            signInResult
        } catch (e: Exception) {
            Log.e("GoogleAuthUiClient", "Sign-In failed", e)
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        Log.d("GoogleAuthUiClient", "Processing Google Sign-In intent")
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            Log.d("GoogleAuthUiClient", "Sign-In successful, user: ${user?.uid}, email: ${user?.email}")
            SignInResult(
                data = user?.run {
                    User(
                        uid = uid,
                        username = "",
                        name = displayName ?: "",
                        email = email ?: "",
                        avatarUrl = photoUrl?.toString() ?: "",
                        phoneNumber = phoneNumber,
                        bio = "",
                        fcmToken = "", // Will be updated later in AuthViewModel
                        lastSeen = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        isOnline = true
                    )
                },
                errorMessage = null
            )

        } catch (e: Exception) {
            Log.e("GoogleAuthUiClient", "Sign-In with intent failed", e)
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}