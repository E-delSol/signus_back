package com.pecadoartesano.features.auth

import org.jetbrains.exposed.v1.core.Table

object RefreshTokenTable : Table("refresh_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")
    val revokedAt = long("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
