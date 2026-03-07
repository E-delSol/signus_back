package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.semaphore.dto.SemaphoreStatusChangedEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import com.pecadoartesano.features.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class StatusServiceTest {

    private val semaphoreRepository = mockk<SemaphoreRepositoryPort>()
    private val partnerLookup = mockk<PartnerLookupPort>()
    private val realtimeNotificationService = mockk<RealtimeNotificationService>()
    private val statusService = StatusServiceImpl(semaphoreRepository, partnerLookup, realtimeNotificationService)

    @Test
    fun `given new status when updateStatus then updates repository and notifies partner`() = runTest {
        // Given
        val expected = Semaphore(
            status = SemaphoreStatus.BUSY,
            userId = "user-1",
            expiration = null,
            duration = null
        )

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY) } returns expected
        every { partnerLookup.findPartnerByUserId("user-1") } returns User(
            id = "partner-1",
            email = "partner-1@test.com",
            passwordHash = "hash",
            displayName = "Partner",
            partnerId = "user-1",
            createdAt = 1L
        )
        coEvery {
            realtimeNotificationService.notifySemaphoreStatusChanged(
                "partner-1",
                any<SemaphoreStatusChangedEvent>()
            )
        } returns true

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.BUSY)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            realtimeNotificationService.notifySemaphoreStatusChanged(
                "partner-1",
                any<SemaphoreStatusChangedEvent>()
            )
        }
    }

    @Test
    fun `given repository throws when updateStatus then propagates error and does not notify`() = runTest {
        // Given
        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY) } throws IllegalStateException("db error")

        // When / Then
        assertFailsWith<IllegalStateException> {
            statusService.updateStatus("user-1", SemaphoreStatus.BUSY)
        }

        coVerify(exactly = 0) { realtimeNotificationService.notifySemaphoreStatusChanged(any(), any()) }
    }

    @Test
    fun `given user without partner when updateStatus then updates repository and skips realtime notification`() = runTest {
        // Given
        val expected = Semaphore(
            status = SemaphoreStatus.AVAILABLE,
            userId = "user-1",
            expiration = null,
            duration = null
        )

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.AVAILABLE) } returns expected
        every { partnerLookup.findPartnerByUserId("user-1") } returns null

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.AVAILABLE)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 0) { realtimeNotificationService.notifySemaphoreStatusChanged(any(), any()) }
    }
}
