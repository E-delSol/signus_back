package com.pecadoartesano.features.auth

data class RefreshTokenSession(
    val id: String,
    val userId: String,
    val tokenHash: String,
    val expiresAt: Long,
    val createdAt: Long,
    val revokedAt: Long? = null
)
