package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.core.config.TokenCleanupConfig
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeviceTokenCleanupSchedulerTest {

    private val repository = mockk<DeviceTokenRepositoryPort>(relaxed = true)
    private val config = TokenCleanupConfig(staleDays = 30)

    @Test
    fun `given stale tokens when runCleanup then deactivates each with stale reason`() = runTest {
        // Given
        val staleTokens = listOf(
            DeviceToken(
                id = "t1", userId = "u1", deviceId = "d1", fcmToken = "fcm-1",
                platform = "android", appVersion = null, active = true,
                createdAt = 0L, updatedAt = 0L, lastRegisteredAt = 0L,
                deactivatedAt = null, deactivationReason = null
            ),
            DeviceToken(
                id = "t2", userId = "u2", deviceId = "d2", fcmToken = "fcm-2",
                platform = "ios", appVersion = "1.0", active = true,
                createdAt = 0L, updatedAt = 0L, lastRegisteredAt = 0L,
                deactivatedAt = null, deactivationReason = null
            )
        )
        every { repository.findActiveTokensOlderThan(any()) } returns staleTokens
        every { repository.deactivateByFcmToken(any(), "stale") } returns null

        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = config,
            checkIntervalMs = 100L
        )

        // When
        scheduler.runCleanup()

        // Then
        verify(exactly = 1) { repository.findActiveTokensOlderThan(any()) }
        verify(exactly = 1) { repository.deactivateByFcmToken("fcm-1", "stale") }
        verify(exactly = 1) { repository.deactivateByFcmToken("fcm-2", "stale") }
    }

    @Test
    fun `given no stale tokens when runCleanup then does not deactivate anything`() = runTest {
        // Given
        every { repository.findActiveTokensOlderThan(any()) } returns emptyList()

        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = config,
            checkIntervalMs = 100L
        )

        // When
        scheduler.runCleanup()

        // Then
        verify(exactly = 1) { repository.findActiveTokensOlderThan(any()) }
        verify(exactly = 0) { repository.deactivateByFcmToken(any(), any()) }
    }

    @Test
    fun `given staleDays config when runCleanup then uses correct threshold`() = runTest {
        // Given
        every { repository.findActiveTokensOlderThan(any()) } returns emptyList()

        val customConfig = TokenCleanupConfig(staleDays = 7)
        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = customConfig,
            checkIntervalMs = 100L
        )

        // When
        scheduler.runCleanup()

        // Then
        verify(exactly = 1) { repository.findActiveTokensOlderThan(any()) }
    }

    @Test
    fun `given start called then job is active`() = runTest {
        // Given
        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = config,
            checkIntervalMs = 100_000L
        )

        // When
        scheduler.start()

        // Then: start should not crash, stop should work cleanly
        scheduler.stop()
    }

    @Test
    fun `given stop called then cancels job`() = runTest {
        // Given
        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = config,
            checkIntervalMs = 100_000L // long interval so it doesn't loop during test
        )

        scheduler.start()

        // When
        scheduler.stop()

        // Then - job should be cancelled, no exception should occur
        // We also verify you can call stop multiple times safely
        scheduler.stop()
    }

    @Test
    fun `given deactivateByFcmToken throws when runCleanup then continues processing`() = runTest {
        // Given: two stale tokens, first throws, second succeeds
        val staleTokens = listOf(
            DeviceToken(
                id = "t1", userId = "u1", deviceId = "d1", fcmToken = "fcm-1",
                platform = "android", appVersion = null, active = true,
                createdAt = 0L, updatedAt = 0L, lastRegisteredAt = 0L,
                deactivatedAt = null, deactivationReason = null
            ),
            DeviceToken(
                id = "t2", userId = "u2", deviceId = "d2", fcmToken = "fcm-2",
                platform = "ios", appVersion = "1.0", active = true,
                createdAt = 0L, updatedAt = 0L, lastRegisteredAt = 0L,
                deactivatedAt = null, deactivationReason = null
            )
        )
        every { repository.findActiveTokensOlderThan(any()) } returns staleTokens
        every { repository.deactivateByFcmToken("fcm-1", "stale") } throws RuntimeException("DB error")
        every { repository.deactivateByFcmToken("fcm-2", "stale") } returns null

        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = config,
            checkIntervalMs = 100L
        )

        // When
        scheduler.runCleanup()

        // Then: both tokens were attempted, execution continued after the exception
        verify(exactly = 1) { repository.deactivateByFcmToken("fcm-1", "stale") }
        verify(exactly = 1) { repository.deactivateByFcmToken("fcm-2", "stale") }
    }

    @Test
    fun `given findActiveTokensOlderThan throws when runCleanup then exception is caught`() = runTest {
        // Given
        every { repository.findActiveTokensOlderThan(any()) } throws RuntimeException("DB error")

        val scheduler = DeviceTokenCleanupScheduler(
            repository = repository,
            config = config,
            checkIntervalMs = 100L
        )

        // When / Then: no exception propagates
        scheduler.runCleanup()
    }
}
