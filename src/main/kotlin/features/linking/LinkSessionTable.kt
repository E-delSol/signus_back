package com.pecadoartesano.features.linking

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object LinkSessionTable : Table("link_sessions") {
    val id = uuid("id")
    val ownerUserId = uuid("owner_user_id")
    val linkCode = varchar("link_code", 32)
    val status = varchar("status", 20)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val confirmedAt = timestamp("confirmed_at").nullable()
    val confirmedByUserId = uuid("confirmed_by_user_id").nullable()

    override val primaryKey = PrimaryKey(id)
}
