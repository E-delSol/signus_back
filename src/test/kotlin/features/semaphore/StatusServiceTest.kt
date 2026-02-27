package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class StatusServiceTest {

    private val semaphoreRepository = mockk<SemaphoreRepositoryPort>()
    private val notificationOrchestrator = mockk<NotificationOrchestrator>()
    private val statusService = StatusService(semaphoreRepository, notificationOrchestrator)

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
        coEvery { notificationOrchestrator.notifyPartnerAboutStatusChange("user-1", SemaphoreStatus.BUSY) } returns Unit

        // When
        val result = statusService.updateStatus("user-1", SemaphoreStatus.BUSY)

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            notificationOrchestrator.notifyPartnerAboutStatusChange("user-1", SemaphoreStatus.BUSY)
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

        coVerify(exactly = 0) { notificationOrchestrator.notifyPartnerAboutStatusChange(any(), any()) }
    }
}
