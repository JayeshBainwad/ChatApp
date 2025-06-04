package com.jsb.chatapp.feature_auth.presentation.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// Step 1: Extension property for Context
val Context.dataStore by preferencesDataStore(name = "user_prefs")

object PreferencesKeys {
    val REMEMBER_ME = booleanPreferencesKey("remember_me")
}

class UserPreferences(private val context: Context) {

    suspend fun saveRememberMe(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REMEMBER_ME] = value
        }
    }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[PreferencesKeys.REMEMBER_ME] ?: false
        }
        .onEach { Log.d("UserPreferences", "Emitting rememberMe: $it") } // Debug log

    suspend fun clearRememberMe() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REMEMBER_ME] = false
        }
    }
}