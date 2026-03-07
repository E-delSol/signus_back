package com.pecadoartesano.features.user.dto

import com.pecadoartesano.features.user.User

fun User.toResponse(): UserResponse =
    UserResponse(
        id = id,
        email = email,
        displayName = displayName,
        partnerId = partnerId,
        createdAt = createdAt
    )
