package com.pecadoartesano.features.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthSessionResponse(
    val accessToken: String,
    val refreshToken: String
)
