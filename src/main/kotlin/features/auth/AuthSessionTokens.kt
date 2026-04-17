package com.pecadoartesano.features.auth

data class AuthSessionTokens(
    val accessToken: String,
    val refreshToken: String
)
