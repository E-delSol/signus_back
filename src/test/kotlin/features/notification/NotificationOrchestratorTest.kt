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
    fun `does nothing when sender has no partner`() = runTest {
        every { partnerLookup.findPartnerByUserId("sender-1") } returns null

        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.AVAILABLE)

        coVerify(exactly = 0) { realtimeNotificationService.notifyPartnerStatusChanged(any(), any()) }
        coVerify(exactly = 0) { pushProvider.sendPush(any(), any(), any(), any()) }
    }

    @Test
    fun `sends realtime notification when partner is connected`() = runTest {
        val partner = partner(id = "partner-1", token = "fcm-token")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery {
            realtimeNotificationService.notifyPartnerStatusChanged(
                "partner-1",
                any<PartnerStatusChangedEvent>()
            )
        } returns true

        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.BUSY)

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
    fun `falls back to push when realtime delivery fails and partner has fcm token`() = runTest {
        val partner = partner(id = "partner-1", token = "fcm-token")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns false
        coEvery { pushProvider.sendPush("partner-1", "fcm-token", any(), any()) } returns true

        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.OFFLINE)

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
    fun `does not send push when realtime fails and partner has no token`() = runTest {
        val partner = partner(id = "partner-1", token = null)
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns false

        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.OFFLINE)

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
