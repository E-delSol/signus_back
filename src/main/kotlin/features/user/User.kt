package com.pecadoartesano.features.user

data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val displayName: String?,
    val partnerId: String? = null,
    val fcmToken: String? = null,
    val createdAt: Long
)
