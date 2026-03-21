package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.ports.DeviceTokenLookupPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnerPushNotificationServiceTest {

    private val deviceTokenLookup = mockk<DeviceTokenLookupPort>()
    private val pushProvider = mockk<PushProvider>()
    private val service = PartnerPushNotificationService(deviceTokenLookup, pushProvider)

    @Test
    fun `given no active tokens when notify user devices then does not call provider`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns emptyList()

        // When
        val result = service.notifyUserDevices("partner-1", "Estado actualizado", "Tu pareja ahora está AVAILABLE")

        // Then
        assertEquals(PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0), result)
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    @Test
    fun `given multiple active tokens when notify user devices then attempts all and counts deliveries`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2", "t3")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } returns true
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns false
        coEvery { pushProvider.sendPush("partner-1", "t3", any(), any()) } returns true

        // When
        val result = service.notifyUserDevices("partner-1", "Estado actualizado", "Tu pareja ahora está BUSY")

        // Then
        assertEquals(PushDispatchResult(totalTokens = 3, attempted = 3, delivered = 2), result)
        coVerify(exactly = 3) { pushProvider.sendPush("partner-1", any(), any(), any()) }
    }

    @Test
    fun `given provider throws for one token when notify user devices then continues with remaining tokens`() = runTest {
        // Given
        every { deviceTokenLookup.findActiveFcmTokensByUserId("partner-1") } returns listOf("t1", "t2")
        coEvery { pushProvider.sendPush("partner-1", "t1", any(), any()) } throws IllegalStateException("network")
        coEvery { pushProvider.sendPush("partner-1", "t2", any(), any()) } returns true

        // When
        val result = service.notifyUserDevices("partner-1", "Estado actualizado", "Tu pareja ahora está OFFLINE")

        // Then
        assertEquals(PushDispatchResult(totalTokens = 2, attempted = 2, delivered = 1), result)
        coVerify(exactly = 2) { pushProvider.sendPush("partner-1", any(), any(), any()) }
    }
}
