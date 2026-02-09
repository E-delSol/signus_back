package com.pecadoartesano.features.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String
)

