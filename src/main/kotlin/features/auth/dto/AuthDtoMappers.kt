package com.pecadoartesano.features.auth.dto

import com.pecadoartesano.features.auth.AuthSessionTokens

fun AuthSessionTokens.toResponse(): AuthSessionResponse =
    AuthSessionResponse(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
