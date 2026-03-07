package com.pecadoartesano.features.linking

import com.pecadoartesano.features.linking.ports.LinkingService
import com.pecadoartesano.features.linking.ports.LinkSessionRepositoryPort
import com.pecadoartesano.features.linking.ports.LinkingUserRepositoryPort
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.UUID

class LinkingServiceImpl(
    private val linkSessionRepository: LinkSessionRepositoryPort,
    private val linkingUserRepository: LinkingUserRepositoryPort,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom()
) : LinkingService {

    companion object {
        private const val SESSION_TTL_SECONDS = 5 * 60L
        private const val LINK_CODE_LENGTH = 10
        private const val LINK_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    override fun createSession(ownerUserId: UUID): LinkSession {
        val now = Instant.now(clock)
        val session = LinkSession(
            id = UUID.randomUUID(),
            ownerUserId = ownerUserId,
            linkCode = generateLinkCode(),
            status = LinkSessionStatus.PENDING,
            expiresAt = now.plusSeconds(SESSION_TTL_SECONDS),
            createdAt = now
        )

        return linkSessionRepository.create(session)
    }

    override fun confirmSessionByLinkCode(linkCode: String, confirmedByUserId: UUID): LinkSession {
        val normalizedCode = linkCode.trim().uppercase()
        if (normalizedCode.isBlank()) {
            throw IllegalArgumentException("linkCode must not be blank")
        }

        val session = linkSessionRepository.findByLinkCode(normalizedCode)
            ?: throw LinkSessionNotFoundException.byLinkCode(normalizedCode)

        if (session.ownerUserId == confirmedByUserId) {
            throw IllegalArgumentException("Cannot confirm linking with the same user")
        }

        if (session.status == LinkSessionStatus.CONFIRMED) {
            throw LinkSessionAlreadyConfirmedException(session.id)
        }

        if (session.status == LinkSessionStatus.EXPIRED || isExpired(session)) {
            if (session.status != LinkSessionStatus.EXPIRED) {
                linkSessionRepository.updateStatus(session.id, LinkSessionStatus.EXPIRED)
            }
            throw LinkSessionExpiredException(session.id)
        }

        linkingUserRepository.linkUsers(session.ownerUserId, confirmedByUserId)

        return linkSessionRepository.markConfirmed(session.id, confirmedByUserId, Instant.now(clock))
            ?: throw LinkSessionNotFoundException.bySessionId(session.id)
    }

    override fun getSession(sessionId: UUID): LinkSession {
        val session = linkSessionRepository.findById(sessionId) ?: throw LinkSessionNotFoundException.bySessionId(sessionId)

        if (session.status == LinkSessionStatus.PENDING && isExpired(session)) {
            return linkSessionRepository.updateStatus(session.id, LinkSessionStatus.EXPIRED)
                ?: throw LinkSessionNotFoundException.bySessionId(session.id)
        }

        return session
    }

    private fun isExpired(session: LinkSession): Boolean = session.expiresAt.isBefore(Instant.now(clock))

    private fun generateLinkCode(): String {
        val chars = CharArray(LINK_CODE_LENGTH) {
            LINK_CODE_ALPHABET[secureRandom.nextInt(LINK_CODE_ALPHABET.length)]
        }
        return String(chars)
    }
}

sealed class LinkingException(message: String) : RuntimeException(message)

class LinkSessionNotFoundException private constructor(message: String) : LinkingException(message) {
    companion object {
        fun bySessionId(sessionId: UUID): LinkSessionNotFoundException =
            LinkSessionNotFoundException("Link session with id $sessionId was not found")

        fun byLinkCode(linkCode: String): LinkSessionNotFoundException =
            LinkSessionNotFoundException("Link session with linkCode $linkCode was not found")
    }
}

class LinkSessionExpiredException(sessionId: UUID) :
    LinkingException("Link session with id $sessionId is expired")

class LinkSessionAlreadyConfirmedException(sessionId: UUID) :
    LinkingException("Link session with id $sessionId is already confirmed")
