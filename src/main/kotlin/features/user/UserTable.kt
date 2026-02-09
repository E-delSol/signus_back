package com.pecadoartesano.features.user

import org.jetbrains.exposed.v1.core.Table

object UserTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 255).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}