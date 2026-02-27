package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

class SemaphoreRepository : SemaphoreRepositoryPort {

    override fun updateUserStatus(userId: String, status: SemaphoreStatus): Semaphore = transaction {
        val existingId = SemaphoreTable
            .selectAll()
            .where { SemaphoreTable.userId eq userId }
            .map { it[SemaphoreTable.id] }
            .singleOrNull()

        val semaphoreId = existingId ?: UUID.randomUUID().toString()

        if (existingId == null) {
            SemaphoreTable.insert {
                it[id] = semaphoreId
                it[SemaphoreTable.userId] = userId
                it[SemaphoreTable.status] = status.name
                it[expiration] = null
                it[duration] = null
            }
        } else {
            SemaphoreTable.update({ SemaphoreTable.id eq semaphoreId }) {
                it[SemaphoreTable.status] = status.name
            }
        }

        Semaphore(
            status = status,
            userId = userId,
            expiration = null,
            duration = null
        )
    }
}
