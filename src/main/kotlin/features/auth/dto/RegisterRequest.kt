package com.pecadoartesano.features.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)
