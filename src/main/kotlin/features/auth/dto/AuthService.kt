package com.pecadoartesano.features.auth.dto

interface AuthService    {
    fun register(
        email: String,
        rawPassword: String,
        displayName: String? = null
    ): String

    fun login(email: String, rawPassword: String): String   
}