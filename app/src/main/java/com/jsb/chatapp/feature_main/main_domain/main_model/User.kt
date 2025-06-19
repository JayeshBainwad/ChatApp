package com.jsb.chatapp.feature_main.main_domain.main_model

data class User(
    val uid: String = "", // Firebase UID
    val username: String = "", // unique!
    val name: String = "", // can be same
    val email: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String? = null,
    val bio: String? = null,
    val fcmToken: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
)