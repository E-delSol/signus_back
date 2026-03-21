package com.pecadoartesano.features.linking

import com.pecadoartesano.features.linking.ports.LinkSessionRepositoryPort
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class LinkSessionRepository : LinkSessionRepositoryPort {

    override fun create(session: LinkSession): LinkSession = transaction {
        LinkSessionTable.insert {
            it[id] = session.id.toKotlinUuid()
            it[ownerUserId] = session.ownerUserId.toKotlinUuid()
            it[linkCode] = session.linkCode
            it[status] = session.status.name
            it[expiresAt] = session.expiresAt
            it[createdAt] = session.createdAt
            it[confirmedAt] = session.confirmedAt
            it[confirmedByUserId] = session.confirmedByUserId?.toKotlinUuid()
        }
        session
    }

    override fun findById(sessionId: UUID): LinkSession? = transaction {
        LinkSessionTable
            .selectAll()
            .where { LinkSessionTable.id eq sessionId.toKotlinUuid() }
            .map(::toLinkSession)
            .singleOrNull()
    }

    override fun findByLinkCode(linkCode: String): LinkSession? = transaction {
        LinkSessionTable
            .selectAll()
            .where { LinkSessionTable.linkCode eq linkCode }
            .map(::toLinkSession)
            .singleOrNull()
    }

    override fun updateStatus(sessionId: UUID, status: LinkSessionStatus): LinkSession? = transaction {
        val updatedRows = LinkSessionTable.update({ LinkSessionTable.id eq sessionId.toKotlinUuid() }) {
            it[LinkSessionTable.status] = status.name
        }

        if (updatedRows == 0) {
            null
        } else {
            LinkSessionTable
                .selectAll()
                .where { LinkSessionTable.id eq sessionId.toKotlinUuid() }
                .map(::toLinkSession)
                .singleOrNull()
        }
    }

    override fun markConfirmed(sessionId: UUID, confirmedByUserId: UUID, confirmedAt: Instant): LinkSession? = transaction {
        val updatedRows = LinkSessionTable.update({ LinkSessionTable.id eq sessionId.toKotlinUuid() }) {
            it[status] = LinkSessionStatus.CONFIRMED.name
            it[LinkSessionTable.confirmedAt] = confirmedAt
            it[LinkSessionTable.confirmedByUserId] = confirmedByUserId.toKotlinUuid()
        }

        if (updatedRows == 0) {
            null
        } else {
            LinkSessionTable
                .selectAll()
                .where { LinkSessionTable.id eq sessionId.toKotlinUuid() }
                .map(::toLinkSession)
                .singleOrNull()
        }
    }

    private fun toLinkSession(row: ResultRow): LinkSession =
        LinkSession(
            id = row[LinkSessionTable.id].toJavaUuid(),
            ownerUserId = row[LinkSessionTable.ownerUserId].toJavaUuid(),
            linkCode = row[LinkSessionTable.linkCode],
            status = LinkSessionStatus.valueOf(row[LinkSessionTable.status]),
            expiresAt = row[LinkSessionTable.expiresAt],
            createdAt = row[LinkSessionTable.createdAt],
            confirmedAt = row[LinkSessionTable.confirmedAt],
            confirmedByUserId = row[LinkSessionTable.confirmedByUserId]?.toJavaUuid()
        )
}
