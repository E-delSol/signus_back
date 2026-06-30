package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationEvent
import com.pecadoartesano.features.notification.ports.NotificationDispatcher
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class StatusServiceImplTest {

    private val semaphoreRepository = mockk<SemaphoreRepositoryPort>()
    private val notificationDispatcher = mockk<NotificationDispatcher>()
    private val partnerLookup = mockk<PartnerLookupPort>()
    private val statusService = StatusServiceImpl(semaphoreRepository, notificationDispatcher, partnerLookup)

    @Test
    fun `given new status when updateStatus then updates repository and dispatches self notification`() = runTest {
        // Given
        val expected = Semaphore(
            status = SemaphoreStatus.BUSY,
            userId = "user-1",
            expiration = null,
            duration = null
        )

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY) } returns expected
        coEvery { notificationDispatcher.dispatch(any()) } returns Unit

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.BUSY)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            notificationDispatcher.dispatch(
                match { event ->
                    event is NotificationEvent.SelfStatusChanged &&
                        event.userId == "user-1" &&
                        event.status == SemaphoreStatus.BUSY
                }
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

        coVerify(exactly = 0) { notificationDispatcher.dispatch(any()) }
    }

    @Test
    fun `given dispatcher fails when updateStatus then still returns updated status`() = runTest {
        // Given
        val expected = Semaphore(
            status = SemaphoreStatus.AVAILABLE,
            userId = "user-1",
            expiration = null,
            duration = null
        )

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.AVAILABLE) } returns expected
        every { partnerLookup.findPartnerByUserId("user-1") } returns null
        coEvery {
            notificationDispatcher.dispatch(any())
        } throws IllegalStateException("notification error")

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.AVAILABLE)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            notificationDispatcher.dispatch(
                match { event ->
                    event is NotificationEvent.SelfStatusChanged &&
                        event.userId == "user-1" &&
                        event.status == SemaphoreStatus.AVAILABLE
                }
            )
        }
    }

    @Test
    fun `given user has partner when updateStatus then dispatches partner notification`() = runTest {
        // Given
        val expected = Semaphore(
            status = SemaphoreStatus.AVAILABLE,
            userId = "user-1",
            expiration = null,
            duration = null
        )
        val partner = User(id = "partner-1", email = "partner@test.com", passwordHash = "hash",
            displayName = "Partner", partnerId = "user-1", fcmToken = "token", createdAt = 0L)

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.AVAILABLE) } returns expected
        every { partnerLookup.findPartnerByUserId("user-1") } returns partner
        coEvery { notificationDispatcher.dispatch(any()) } returns Unit

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.AVAILABLE)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            notificationDispatcher.dispatch(
                match { event ->
                    event is NotificationEvent.PartnerStatusChanged &&
                        event.actorUserId == "user-1" &&
                        event.recipientUserId == "partner-1" &&
                        event.status == SemaphoreStatus.AVAILABLE
                }
            )
        }
    }

    @Test
    fun `given user has no partner when updateStatus then does not dispatch partner notification`() = runTest {
        // Given
        val expected = Semaphore(
            status = SemaphoreStatus.BUSY,
            userId = "user-1",
            expiration = null,
            duration = null
        )

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY) } returns expected
        every { partnerLookup.findPartnerByUserId("user-1") } returns null
        coEvery { notificationDispatcher.dispatch(any()) } returns Unit

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.BUSY)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            notificationDispatcher.dispatch(
                match { event ->
                    event is NotificationEvent.SelfStatusChanged
                }
            )
        }
        coVerify(exactly = 0) {
            notificationDispatcher.dispatch(
                match { event ->
                    event is NotificationEvent.PartnerStatusChanged
                }
            )
        }
    }
}
