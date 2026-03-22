package com.pecadoartesano.features.user.dto

import com.pecadoartesano.features.semaphore.UserState
import com.pecadoartesano.features.user.User

fun User.toResponse(): UserResponse =
    UserResponse(
        id = id,
        email = email,
        displayName = displayName,
        partnerId = partnerId,
        createdAt = createdAt
    )

fun UserState.toMeResponse(): MeResponse =
    MeResponse(
        id = id,
        status = status,
        statusExpiration = statusExpiration,
        statusDuration = statusDuration,
        partnerId = partnerId,
        displayName = displayName
    )
