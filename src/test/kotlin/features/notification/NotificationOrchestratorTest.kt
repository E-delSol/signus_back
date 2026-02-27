package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.RealtimeNotificationService
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class NotificationOrchestratorTest {

    private val partnerLookup = mockk<PartnerLookupPort>()
    private val realtimeNotificationService = mockk<RealtimeNotificationService>()
    private val pushProvider = mockk<PushProvider>()
    private val orchestrator = NotificationOrchestrator(partnerLookup, realtimeNotificationService, pushProvider)

    @Test
    fun `given sender without partner when notify then does nothing`() = runTest {
        // Given
        every { partnerLookup.findPartnerByUserId("sender-1") } returns null

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.AVAILABLE)

        // Then
        coVerify(exactly = 0) { realtimeNotificationService.notifyPartnerStatusChanged(any(), any()) }
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    @Test
    fun `given partner connected when notify then sends realtime notification only`() = runTest {
        // Given
        val partner = partner(id = "partner-1", token = "fcm-token")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery {
            realtimeNotificationService.notifyPartnerStatusChanged(
                "partner-1",
                any<PartnerStatusChangedEvent>()
            )
        } returns true

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.BUSY)

        // Then
        coVerify(exactly = 1) {
            realtimeNotificationService.notifyPartnerStatusChanged(
                "partner-1",
                PartnerStatusChangedEvent(
                    senderId = "sender-1",
                    partnerId = "partner-1",
                    status = SemaphoreStatus.BUSY
                )
            )
        }
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    @Test
    fun `given realtime fails and partner has token when notify then sends push`() = runTest {
        // Given
        val partner = partner(id = "partner-1", token = "fcm-token")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns false
        coEvery { pushProvider.sendPush("partner-1", "fcm-token", any(), any()) } returns true

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.OFFLINE)

        // Then
        coVerify(exactly = 1) {
            pushProvider.sendPush(
                targetUserId = "partner-1",
                token = "fcm-token",
                title = "Estado actualizado",
                body = "Tu pareja ahora está OFFLINE"
            )
        }
    }

    @Test
    fun `given realtime succeeds and partner has token when notify then does not send push`() = runTest {
        // Given
        val partner = partner(id = "partner-1", token = "fcm-token")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns true

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.AVAILABLE)

        // Then
        coVerify(exactly = 1) {
            realtimeNotificationService.notifyPartnerStatusChanged(
                "partner-1",
                PartnerStatusChangedEvent(
                    senderId = "sender-1",
                    partnerId = "partner-1",
                    status = SemaphoreStatus.AVAILABLE
                )
            )
        }
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    @Test
    fun `given realtime fails and partner has no token when notify then does not send push`() = runTest {
        // Given
        val partner = partner(id = "partner-1", token = null)
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns false

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.OFFLINE)

        // Then
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    private fun partner(id: String, token: String?) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        displayName = "Partner",
        fcmToken = token,
        createdAt = 1L
    )
}
