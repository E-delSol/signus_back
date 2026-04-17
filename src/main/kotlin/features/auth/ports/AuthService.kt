package com.pecadoartesano.features.auth.ports

import com.pecadoartesano.features.auth.AuthSessionTokens

interface AuthService {
    fun register(
        email: String,
        rawPassword: String,
        displayName: String? = null
    ): AuthSessionTokens

    fun login(email: String, rawPassword: String): AuthSessionTokens

    fun refresh(refreshToken: String): String

    fun logout(userId: String, refreshToken: String)
}
