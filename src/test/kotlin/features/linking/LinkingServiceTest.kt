package com.pecadoartesano.features.linking

import com.pecadoartesano.features.linking.LinkSession
import com.pecadoartesano.features.linking.LinkSessionAlreadyConfirmedException
import com.pecadoartesano.features.linking.LinkSessionExpiredException
import com.pecadoartesano.features.linking.LinkSessionNotFoundException
import com.pecadoartesano.features.linking.LinkSessionStatus
import com.pecadoartesano.features.linking.LinkingServiceImpl
import com.pecadoartesano.features.linking.ports.LinkSessionRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinkingServiceTest {

    private val repository = mockk<LinkSessionRepositoryPort>()
    private val fixedNow = Instant.parse("2026-03-06T12:00:00Z")
    private val clock: Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val service = LinkingServiceImpl(repository, clock)

    @Test
    fun `given owner user when createSession then returns pending session with linkCode and 5 minute expiration`() {
        // Given
        every { repository.create(any()) } answers { firstArg() }

        // When
        val ownerUserId = UUID.randomUUID()
        val session = service.createSession(ownerUserId)

        // Then
        assertEquals(ownerUserId, session.ownerUserId)
        assertEquals(LinkSessionStatus.PENDING, session.status)
        assertEquals(fixedNow, session.createdAt)
        assertEquals(fixedNow.plusSeconds(300), session.expiresAt)
        assertEquals(10, session.linkCode.length)
        assertTrue(session.linkCode.matches(Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{10}$")))
    }

    @Test
    fun `given valid pending session when confirmSessionByLinkCode then marks session as confirmed`() {
        // Given
        val ownerUserId = UUID.randomUUID()
        val confirmerUserId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val linkCode = "A7K9P3XQ2M"
        val pending = LinkSession(
            id = sessionId,
            ownerUserId = ownerUserId,
            linkCode = linkCode,
            status = LinkSessionStatus.PENDING,
            expiresAt = fixedNow.plusSeconds(120),
            createdAt = fixedNow.minusSeconds(60)
        )
        val confirmed = pending.copy(
            status = LinkSessionStatus.CONFIRMED,
            confirmedAt = fixedNow,
            confirmedByUserId = confirmerUserId
        )

        every { repository.findByLinkCode(linkCode) } returns pending
        every { repository.markConfirmed(sessionId, confirmerUserId, fixedNow) } returns confirmed

        // When
        val result = service.confirmSessionByLinkCode(linkCode, confirmerUserId)

        // Then
        assertEquals(LinkSessionStatus.CONFIRMED, result.status)
        assertEquals(confirmerUserId, result.confirmedByUserId)
        assertEquals(fixedNow, result.confirmedAt)
    }

    @Test
    fun `given missing session when confirmSessionByLinkCode then throws not found`() {
        // Given
        every { repository.findByLinkCode("MISSING1234") } returns null

        // When / Then
        assertFailsWith<LinkSessionNotFoundException> {
            service.confirmSessionByLinkCode("MISSING1234", UUID.randomUUID())
        }
    }

    @Test
    fun `given expired pending session when confirmSessionByLinkCode then marks expired and throws`() {
        // Given
        val sessionId = UUID.randomUUID()
        val expiredPending = LinkSession(
            id = sessionId,
            ownerUserId = UUID.randomUUID(),
            linkCode = "A7K9P3XQ2M",
            status = LinkSessionStatus.PENDING,
            expiresAt = fixedNow.minusSeconds(1),
            createdAt = fixedNow.minusSeconds(301)
        )

        every { repository.findByLinkCode("A7K9P3XQ2M") } returns expiredPending
        every { repository.updateStatus(sessionId, LinkSessionStatus.EXPIRED) } returns expiredPending.copy(status = LinkSessionStatus.EXPIRED)

        // When / Then
        assertFailsWith<LinkSessionExpiredException> {
            service.confirmSessionByLinkCode("A7K9P3XQ2M", UUID.randomUUID())
        }

        verify(exactly = 1) { repository.updateStatus(sessionId, LinkSessionStatus.EXPIRED) }
    }

    @Test
    fun `given confirmed session when confirmSessionByLinkCode then throws conflict exception`() {
        // Given
        val confirmed = LinkSession(
            id = UUID.randomUUID(),
            ownerUserId = UUID.randomUUID(),
            linkCode = "A7K9P3XQ2M",
            status = LinkSessionStatus.CONFIRMED,
            expiresAt = fixedNow.plusSeconds(100),
            createdAt = fixedNow.minusSeconds(20),
            confirmedAt = fixedNow.minusSeconds(10),
            confirmedByUserId = UUID.randomUUID()
        )

        every { repository.findByLinkCode("A7K9P3XQ2M") } returns confirmed

        // When / Then
        assertFailsWith<LinkSessionAlreadyConfirmedException> {
            service.confirmSessionByLinkCode("A7K9P3XQ2M", UUID.randomUUID())
        }
    }
}
