package com.pecadoartesano.features.notification

import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import com.pecadoartesano.features.notification.ports.DeviceTokenLookupPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartnerPushNotificationServiceTest {

    private val deviceTokenLookup = mockk<DeviceTokenLookupPort>()
    private val deviceTokenRepository = mockk<DeviceTokenRepositoryPort>(relaxed = true)
    private val pushProvider = mockk<PushProvider>()
    private val service = PartnerPushNotificationService(
        deviceTokenLookup = deviceTokenLookup,
        pushProvider = pushProvider,
        deviceTokenRepository = deviceTokenRepository
    )

    @Test
    fun `given no active tokens when notify user then does not call provider`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns emptyList()

        // When
        val result = service.notifyUser("partner-1", "Estado actualizado", "Tu pareja ahora está AVAILABLE")

        // Then
        assertEquals(PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0), result)
        assertTrue(result.results.isEmpty())
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    @Test
    fun `given multiple active tokens when notify user then attempts all and counts deliveries`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2", "t3")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns PushResult.Success("t1")
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns PushResult.PermanentFailure("t2", "unregistered", "UNREGISTERED")
        coEvery { pushProvider.sendPush("partner-1", "t3", any(), any()) } returns PushResult.Success("t3")

        // When
        val result = service.notifyUser("partner-1", "Estado actualizado", "Tu pareja ahora está BUSY")

        // Then
        assertEquals(3, result.totalTokens)
        assertEquals(3, result.attempted)
        assertEquals(2, result.delivered)
        assertEquals(3, result.results.size)
        coVerify(exactly = 3) { pushProvider.sendPush("partner-1", any(), any(), any()) }
    }

    @Test
    fun `given provider throws for one token when notify user then continues with remaining tokens`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } throws IllegalStateException("network")
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns PushResult.Success("t2")

        // When
        val result = service.notifyUser("partner-1", "Estado actualizado", "Tu pareja ahora está OFFLINE")

        // Then
        assertEquals(2, result.totalTokens)
        assertEquals(2, result.attempted)
        assertEquals(1, result.delivered)
        assertEquals(2, result.results.size)
        coVerify(exactly = 2) { pushProvider.sendPush("partner-1", any(), any(), any()) }
    }

    // --- Deactivation tests (T5.15 + T5.18) ---

    @Test
    fun `given permanent failure when notify user then deactivates token`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns
            PushResult.PermanentFailure("t1", "unregistered", "UNREGISTERED")
        every { deviceTokenRepository.deactivateByFcmToken("t1", "unregistered") } returns null

        // When
        val result = service.notifyUser("partner-1", "Título", "Cuerpo")

        // Then
        assertEquals(1, result.totalTokens)
        assertEquals(0, result.delivered)
        assertEquals(1, result.permanentFailures.size)
        assertEquals(0, result.temporaryFailures.size)
        assertTrue(result.isCompleted)
        verify(exactly = 1) { deviceTokenRepository.deactivateByFcmToken("t1", "unregistered") }
    }

    @Test
    fun `given temporary failure when notify user then does NOT deactivate token`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns
            PushResult.TemporaryFailure("t1", "unavailable", "UNAVAILABLE")

        // When
        val result = service.notifyUser("partner-1", "Título", "Cuerpo")

        // Then
        assertEquals(1, result.totalTokens)
        assertEquals(0, result.delivered)
        assertEquals(0, result.permanentFailures.size)
        assertEquals(1, result.temporaryFailures.size)
        assertFalse(result.isCompleted)
        verify(exactly = 0) { deviceTokenRepository.deactivateByFcmToken(any(), any()) }
    }

    @Test
    fun `given success when notify user then does NOT deactivate token`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns
            PushResult.Success("t1")

        // When
        val result = service.notifyUser("partner-1", "Título", "Cuerpo")

        // Then
        assertEquals(1, result.totalTokens)
        assertEquals(1, result.delivered)
        assertEquals(0, result.permanentFailures.size)
        assertEquals(0, result.temporaryFailures.size)
        assertTrue(result.isCompleted)
        verify(exactly = 0) { deviceTokenRepository.deactivateByFcmToken(any(), any()) }
    }

    @Test
    fun `given mixed results when notify user then deactivates only permanent failures`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2", "t3")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns PushResult.Success("t1")
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns
            PushResult.PermanentFailure("t2", "invalid_argument", "INVALID_ARGUMENT")
        coEvery { pushProvider.sendPush("partner-1", "t3", any(), any()) } returns
            PushResult.TemporaryFailure("t3", "quota_exceeded", "QUOTA_EXCEEDED")
        every { deviceTokenRepository.deactivateByFcmToken("t2", "invalid_argument") } returns null

        // When
        val result = service.notifyUser("partner-1", "Título", "Cuerpo")

        // Then
        assertEquals(3, result.totalTokens)
        assertEquals(1, result.delivered)
        assertEquals(1, result.permanentFailures.size)
        assertEquals(1, result.temporaryFailures.size)
        assertFalse(result.isCompleted)

        // Only T2 should be deactivated
        verify(exactly = 1) { deviceTokenRepository.deactivateByFcmToken("t2", "invalid_argument") }
        verify(exactly = 0) { deviceTokenRepository.deactivateByFcmToken("t1", any()) }
        verify(exactly = 0) { deviceTokenRepository.deactivateByFcmToken("t3", any()) }
    }

    @Test
    fun `given multiple permanent failures when notify user then deactivates each`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns
            PushResult.PermanentFailure("t1", "unregistered", "UNREGISTERED")
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns
            PushResult.PermanentFailure("t2", "invalid_argument", "INVALID_ARGUMENT")
        every { deviceTokenRepository.deactivateByFcmToken("t1", "unregistered") } returns null
        every { deviceTokenRepository.deactivateByFcmToken("t2", "invalid_argument") } returns null

        // When
        val result = service.notifyUser("partner-1", "Título", "Cuerpo")

        // Then
        assertEquals(2, result.totalTokens)
        assertEquals(0, result.delivered)
        assertEquals(2, result.permanentFailures.size)
        assertEquals(0, result.temporaryFailures.size)
        assertTrue(result.isCompleted)
        verify(exactly = 1) { deviceTokenRepository.deactivateByFcmToken("t1", "unregistered") }
        verify(exactly = 1) { deviceTokenRepository.deactivateByFcmToken("t2", "invalid_argument") }
    }

    @Test
    fun `given all successes when notify user then result has no failures and is completed`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns PushResult.Success("t1")
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns PushResult.Success("t2")

        // When
        val result = service.notifyUser("partner-1", "Título", "Cuerpo")

        // Then
        assertEquals(2, result.totalTokens)
        assertEquals(2, result.delivered)
        assertEquals(0, result.permanentFailures.size)
        assertEquals(0, result.temporaryFailures.size)
        assertTrue(result.isCompleted)
        assertTrue(result.results.all { it is PushResult.Success })
    }
}
