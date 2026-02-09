package com.pecadoartesano.features.user

data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val displayName: String?,
    val createdAt: Long
) {
        fun toResponse() = UserResponse(
            id = id,
            email = email,
            displayName = displayName,
            createdAt = createdAt
        )
}
