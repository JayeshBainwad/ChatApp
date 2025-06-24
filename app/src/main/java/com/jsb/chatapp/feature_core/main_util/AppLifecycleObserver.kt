package com.jsb.chatapp.feature_core.main_util

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val userStatusManager: UserStatusManager,
    private val firebaseAuth: FirebaseAuth
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInForeground = false

    companion object {
        private const val TAG = "AppLifecycleObserver"
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App comes to foreground
        Log.d(TAG, "App came to foreground")
        firebaseAuth.currentUser?.uid?.let { userId ->
            scope.launch {
                userStatusManager.setUserOnline(userId)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App goes to background
        Log.d(TAG, "App went to background")
        firebaseAuth.currentUser?.uid?.let { userId ->
            scope.launch {
                userStatusManager.setUserOffline(userId)
            }
        }
    }

    fun isAppInForeground(): Boolean = isInForeground
}