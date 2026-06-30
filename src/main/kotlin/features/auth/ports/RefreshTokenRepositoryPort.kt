package com.pecadoartesano.features.auth.ports

import com.pecadoartesano.features.auth.RefreshTokenSession

interface RefreshTokenRepositoryPort {
    fun create(refreshTokenSession: RefreshTokenSession): RefreshTokenSession
    fun findByTokenHash(tokenHash: String): RefreshTokenSession?
    fun revokeById(refreshTokenSessionId: String, revokedAt: Long): Boolean
}
