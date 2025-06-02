package com.jsb.chatapp.feature_auth.domain.model

data class User(
    val uid: String = "", // Firebase UID
    val username: String = "", // unique!
    val name: String = "", // can be same
    val email: String = "",
    val avatarUrl: String = "", // optional
    val phoneNumber: String? = null,
    val bio: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)