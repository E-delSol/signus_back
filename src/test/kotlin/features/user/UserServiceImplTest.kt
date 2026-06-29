package com.pecadoartesano.features.user

import com.pecadoartesano.features.notification.NotificationEvent
import com.pecadoartesano.features.notification.ports.NotificationDispatcher
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
    private val notificationDispatcher = mockk<NotificationDispatcher>()
    private val service = UserServiceImpl(userRepository, semaphoreRepository, notificationDispatcher)

    @Test
    fun `given linked user when unlink current user then unlinks and dispatches notification`() = runTest {
        // Given
        every { userRepository.unlinkUsers("user-1") } returns "user-2"
        coEvery { notificationDispatcher.dispatch(any()) } returns Unit

        // When
        service.unlinkCurrentUser("user-1")

        // Then
        coVerify(exactly = 1) {
            notificationDispatcher.dispatch(
                match { event ->
                    event is NotificationEvent.PartnerUnlinked &&
                        event.actorUserId == "user-1" &&
                        event.recipientUserId == "user-2"
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
        coVerify(exactly = 0) { notificationDispatcher.dispatch(any()) }
    }
}
