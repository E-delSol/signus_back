package com.pecadoartesano.features.linking.ports

import com.pecadoartesano.features.linking.LinkSession
import com.pecadoartesano.features.linking.LinkSessionStatus
import java.time.Instant
import java.util.UUID

interface LinkSessionRepositoryPort {
    fun create(session: LinkSession): LinkSession
    fun findById(sessionId: UUID): LinkSession?
    fun findByLinkCode(linkCode: String): LinkSession?
    fun updateStatus(sessionId: UUID, status: LinkSessionStatus): LinkSession?
    fun markConfirmed(sessionId: UUID, confirmedByUserId: UUID, confirmedAt: Instant): LinkSession?
}
