package com.pecadoartesano.features.auth

import com.pecadoartesano.features.auth.ports.RefreshTokenRepositoryPort
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class RefreshTokenRepository : RefreshTokenRepositoryPort {

    override fun create(refreshTokenSession: RefreshTokenSession): RefreshTokenSession = transaction {
        RefreshTokenTable.insert {
            it[id] = refreshTokenSession.id
            it[userId] = refreshTokenSession.userId
            it[tokenHash] = refreshTokenSession.tokenHash
            it[expiresAt] = refreshTokenSession.expiresAt
            it[createdAt] = refreshTokenSession.createdAt
            it[revokedAt] = refreshTokenSession.revokedAt
        }
        refreshTokenSession
    }

    override fun findByTokenHash(tokenHash: String): RefreshTokenSession? = transaction {
        RefreshTokenTable
            .selectAll()
            .where { RefreshTokenTable.tokenHash eq tokenHash }
            .map(::toRefreshTokenSession)
            .singleOrNull()
    }

    override fun revokeById(refreshTokenSessionId: String, revokedAt: Long): Boolean = transaction {
        RefreshTokenTable.update({ RefreshTokenTable.id eq refreshTokenSessionId }) {
            it[RefreshTokenTable.revokedAt] = revokedAt
        } > 0
    }

    private fun toRefreshTokenSession(row: ResultRow): RefreshTokenSession =
        RefreshTokenSession(
            id = row[RefreshTokenTable.id],
            userId = row[RefreshTokenTable.userId],
            tokenHash = row[RefreshTokenTable.tokenHash],
            expiresAt = row[RefreshTokenTable.expiresAt],
            createdAt = row[RefreshTokenTable.createdAt],
            revokedAt = row[RefreshTokenTable.revokedAt]
        )
}
