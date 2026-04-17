package com.pecadoartesano.features.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshSessionResponse(
    val accessToken: String
)
