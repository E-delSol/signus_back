package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationEvent
import com.pecadoartesano.features.notification.ports.NotificationDispatcher
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
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
    private val statusService = StatusServiceImpl(semaphoreRepository, notificationDispatcher)

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
}
