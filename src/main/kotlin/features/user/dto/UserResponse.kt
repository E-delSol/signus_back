package com.pecadoartesano.features.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String?,
    val partnerId: String? = null,
    val createdAt: Long
)
