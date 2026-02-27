package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class StatusServiceTest {

    private val semaphoreRepository = mockk<SemaphoreRepositoryPort>()
    private val notificationOrchestrator = mockk<NotificationOrchestrator>()
    private val statusService = StatusService(semaphoreRepository, notificationOrchestrator)

    @Test
    fun `updateStatus updates repository and notifies partner`() = runTest {
        val expected = Semaphore(
            status = SemaphoreStatus.BUSY,
            userId = "user-1",
            expiration = null,
            duration = null
        )

        every { semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY) } returns expected
        coEvery { notificationOrchestrator.notifyPartnerAboutStatusChange("user-1", SemaphoreStatus.BUSY) } returns Unit

        val result = statusService.updateStatus("user-1", SemaphoreStatus.BUSY)

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            notificationOrchestrator.notifyPartnerAboutStatusChange("user-1", SemaphoreStatus.BUSY)
        }
    }
}
