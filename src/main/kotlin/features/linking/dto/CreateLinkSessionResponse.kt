package com.pecadoartesano.features.linking.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkSessionResponse(
    val sessionId: String,
    val linkCode: String,
    val expiresAt: String
)
