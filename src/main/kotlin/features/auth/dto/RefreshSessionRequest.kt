package com.pecadoartesano.features.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshSessionRequest(
    val refreshToken: String
)
