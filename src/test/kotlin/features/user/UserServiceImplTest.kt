package com.pecadoartesano.features.user

import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UserServiceImplTest {

    private val userRepository = mockk<UserRepository>()
    private val semaphoreRepository = mockk<SemaphoreRepositoryPort>()
    private val realtimeNotificationService = mockk<RealtimeNotificationService>()
    private val service = UserServiceImpl(userRepository, semaphoreRepository, realtimeNotificationService)

    @Test
    fun `given linked user when unlink current user then unlinks and notifies partner`() = runTest {
        // Given
        every { userRepository.unlinkUsers("user-1") } returns "user-2"
        coEvery { realtimeNotificationService.notifyPartnerUnlinked("user-2", any<PartnerUnlinkedEvent>()) } returns true

        // When
        service.unlinkCurrentUser("user-1")

        // Then
        coVerify(exactly = 1) {
            realtimeNotificationService.notifyPartnerUnlinked(
                "user-2",
                match {
                    it.partnerId == "user-1" &&
                        it.timestamp > 0
                }
            )
        }
    }

    @Test
    fun `given user without linked partner when unlink current user then throws and does not notify`() = runTest {
        // Given
        every { userRepository.unlinkUsers("user-1") } returns null

        // When / Then
        assertFailsWith<IllegalStateException> {
            service.unlinkCurrentUser("user-1")
        }
        coVerify(exactly = 0) { realtimeNotificationService.notifyPartnerUnlinked(any(), any()) }
    }
}
