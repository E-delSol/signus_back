package com.pecadoartesano.features.semaphore

import org.jetbrains.exposed.v1.core.Table

object SemaphoreTable : Table("semaphores") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val status = varchar("status", 20)
    val expiration = long("expiration").nullable()
    val duration = long("duration").nullable()

    override val primaryKey = PrimaryKey(id)
}
