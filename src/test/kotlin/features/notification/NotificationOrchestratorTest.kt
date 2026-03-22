package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.SelfStatusChangedEvent
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
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
    private val partnerPushNotificationService = mockk<PartnerPushNotificationService>()
    private val orchestrator = NotificationOrchestrator(partnerLookup, realtimeNotificationService, partnerPushNotificationService)

    @Test
    fun `given sender without partner when notify then does nothing`() = runTest {
        // Given
        every { partnerLookup.findPartnerByUserId("sender-1") } returns null
        coEvery { realtimeNotificationService.notifySelfStatusChanged("sender-1", any()) } returns true

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.AVAILABLE)

        // Then
        coVerify(exactly = 1) {
            realtimeNotificationService.notifySelfStatusChanged(
                "sender-1",
                match {
                    it.type == "SELF_STATUS_CHANGED" &&
                        it.userId == "sender-1" &&
                        it.status == SemaphoreStatus.AVAILABLE &&
                        it.statusExpiration == null &&
                        it.timestamp > 0
                }
            )
        }
        coVerify(exactly = 0) { realtimeNotificationService.notifyPartnerStatusChanged(any(), any()) }
        coVerify(exactly = 0) { partnerPushNotificationService.notifyUserDevices(any(), any(), any()) }
    }

    @Test
    fun `given partner connected when notify then sends realtime notification only`() = runTest {
        // Given
        val partner = partner(id = "partner-1")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery {
            realtimeNotificationService.notifySelfStatusChanged(
                "sender-1",
                any<SelfStatusChangedEvent>()
            )
        } returns true
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
            realtimeNotificationService.notifySelfStatusChanged(
                "sender-1",
                match {
                    it.type == "SELF_STATUS_CHANGED" &&
                        it.userId == "sender-1" &&
                        it.status == SemaphoreStatus.BUSY &&
                        it.statusExpiration == null &&
                        it.timestamp > 0
                }
            )
        }
        coVerify(exactly = 1) {
            realtimeNotificationService.notifyPartnerStatusChanged(
                "partner-1",
                match {
                    it.type == "PARTNER_STATUS_CHANGED" &&
                        it.partnerId == "sender-1" &&
                        it.status == SemaphoreStatus.BUSY &&
                        it.statusExpiration == null &&
                        it.timestamp > 0
                }
            )
        }
        coVerify(exactly = 0) { partnerPushNotificationService.notifyUserDevices(any(), any(), any()) }
    }

    @Test
    fun `given realtime fails when notify then triggers push service`() = runTest {
        // Given
        val partner = partner(id = "partner-1")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifySelfStatusChanged("sender-1", any()) } returns true
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns false
        coEvery { partnerPushNotificationService.notifyUserDevices("partner-1", any(), any()) } returns
            PushDispatchResult(totalTokens = 1, attempted = 1, delivered = 1)

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.OFFLINE)

        // Then
        coVerify(exactly = 1) {
            partnerPushNotificationService.notifyUserDevices(
                targetUserId = "partner-1",
                title = "Estado actualizado",
                body = "Tu pareja ahora está OFFLINE"
            )
        }
    }

    @Test
    fun `given realtime succeeds when notify then does not trigger push service`() = runTest {
        // Given
        val partner = partner(id = "partner-1")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifySelfStatusChanged("sender-1", any()) } returns true
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns true

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.AVAILABLE)

        // Then
        coVerify(exactly = 1) {
            realtimeNotificationService.notifyPartnerStatusChanged(
                "partner-1",
                match {
                    it.type == "PARTNER_STATUS_CHANGED" &&
                        it.partnerId == "sender-1" &&
                        it.status == SemaphoreStatus.AVAILABLE &&
                        it.statusExpiration == null &&
                        it.timestamp > 0
                }
            )
        }
        coVerify(exactly = 0) { partnerPushNotificationService.notifyUserDevices(any(), any(), any()) }
    }

    @Test
    fun `given realtime fails and no active tokens when notify then still delegates to push service`() = runTest {
        // Given
        val partner = partner(id = "partner-1")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifySelfStatusChanged("sender-1", any()) } returns true
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns false
        coEvery { partnerPushNotificationService.notifyUserDevices("partner-1", any(), any()) } returns
            PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.OFFLINE)

        // Then
        coVerify(exactly = 1) { partnerPushNotificationService.notifyUserDevices("partner-1", any(), any()) }
    }

    @Test
    fun `given realtime throws when notify then falls back to push service`() = runTest {
        // Given
        val partner = partner(id = "partner-1")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery { realtimeNotificationService.notifySelfStatusChanged("sender-1", any()) } returns true
        coEvery {
            realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any())
        } throws IllegalStateException("socket closed")
        coEvery { partnerPushNotificationService.notifyUserDevices("partner-1", any(), any()) } returns
            PushDispatchResult(totalTokens = 1, attempted = 1, delivered = 1)

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.BUSY)

        // Then
        coVerify(exactly = 1) { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) }
        coVerify(exactly = 1) {
            partnerPushNotificationService.notifyUserDevices(
                targetUserId = "partner-1",
                title = "Estado actualizado",
                body = "Tu pareja ahora está BUSY"
            )
        }
    }

    @Test
    fun `given self realtime throws when notify then still notifies partner`() = runTest {
        // Given
        val partner = partner(id = "partner-1")
        every { partnerLookup.findPartnerByUserId("sender-1") } returns partner
        coEvery {
            realtimeNotificationService.notifySelfStatusChanged("sender-1", any())
        } throws IllegalStateException("socket closed")
        coEvery { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) } returns true

        // When
        orchestrator.notifyPartnerAboutStatusChange("sender-1", SemaphoreStatus.BUSY)

        // Then
        coVerify(exactly = 1) { realtimeNotificationService.notifyPartnerStatusChanged("partner-1", any()) }
        coVerify(exactly = 0) { partnerPushNotificationService.notifyUserDevices(any(), any(), any()) }
    }

    private fun partner(id: String) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        displayName = "Partner",
        createdAt = 1L
    )
}
