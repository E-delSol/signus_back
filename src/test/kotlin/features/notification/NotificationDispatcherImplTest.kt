package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
import com.pecadoartesano.features.notification.dto.SelfStatusChangedEvent
import com.pecadoartesano.features.notification.ports.NotificationMapper
import com.pecadoartesano.features.notification.ports.NotificationPayload
import com.pecadoartesano.features.notification.ports.PushNotificationService
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class NotificationDispatcherImplTest {

    private val realtimeService = mockk<RealtimeNotificationService>()
    private val pushService = mockk<PushNotificationService>()
    private val mapper = mockk<NotificationMapper>()
    private val dispatcher = NotificationDispatcherImpl(realtimeService, pushService, mapper)

    @Test
    fun `given partner online when PartnerStatusChanged then sends realtime only`() = runTest {
        // Given
        val event = NotificationEvent.PartnerStatusChanged(
            actorUserId = "sender-1",
            recipientUserId = "partner-1",
            status = SemaphoreStatus.AVAILABLE
        )
        coEvery {
            realtimeService.notifyPartnerStatusChanged("partner-1", any<PartnerStatusChangedEvent>())
        } returns true

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) {
            realtimeService.notifyPartnerStatusChanged(
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
        coVerify(exactly = 0) { pushService.notifyUser(any(), any(), any(), any()) }
    }

    @Test
    fun `given partner offline when PartnerStatusChanged then falls back to push`() = runTest {
        // Given
        val event = NotificationEvent.PartnerStatusChanged(
            actorUserId = "sender-1",
            recipientUserId = "partner-1",
            status = SemaphoreStatus.BUSY
        )
        coEvery {
            realtimeService.notifyPartnerStatusChanged("partner-1", any<PartnerStatusChangedEvent>())
        } returns false
        every { mapper.toPayload(event) } returns NotificationPayload(
            title = "Status update",
            body = "Partner is BUSY",
            data = mapOf("type" to "PARTNER_STATUS_CHANGED")
        )
        coEvery { pushService.notifyUser(any(), any(), any(), any()) } returns PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) {
            realtimeService.notifyPartnerStatusChanged("partner-1", any())
        }
        coVerify(exactly = 1) {
            pushService.notifyUser(
                targetUserId = "partner-1",
                title = "Status update",
                body = "Partner is BUSY",
                data = mapOf("type" to "PARTNER_STATUS_CHANGED")
            )
        }
    }

    @Test
    fun `given realtime fails when PartnerStatusChanged then falls back to push`() = runTest {
        // Given
        val event = NotificationEvent.PartnerStatusChanged(
            actorUserId = "sender-1",
            recipientUserId = "partner-1",
            status = SemaphoreStatus.OFFLINE
        )
        coEvery {
            realtimeService.notifyPartnerStatusChanged("partner-1", any<PartnerStatusChangedEvent>())
        } returns false
        every { mapper.toPayload(event) } returns NotificationPayload(
            title = "Status update",
            body = "Partner is OFFLINE",
            data = emptyMap()
        )
        coEvery { pushService.notifyUser(any(), any(), any(), any()) } returns PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) {
            pushService.notifyUser("partner-1", "Status update", "Partner is OFFLINE", emptyMap())
        }
    }

    @Test
    fun `given realtime throws exception when PartnerStatusChanged then falls back to push`() = runTest {
        // Given
        val event = NotificationEvent.PartnerStatusChanged(
            actorUserId = "sender-1",
            recipientUserId = "partner-1",
            status = SemaphoreStatus.BUSY
        )
        coEvery {
            realtimeService.notifyPartnerStatusChanged("partner-1", any<PartnerStatusChangedEvent>())
        } throws IllegalStateException("socket closed")
        every { mapper.toPayload(event) } returns NotificationPayload(
            title = "Status update",
            body = "Partner is BUSY",
            data = mapOf("reason" to "realtime_error")
        )
        coEvery { pushService.notifyUser(any(), any(), any(), any()) } returns PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) { realtimeService.notifyPartnerStatusChanged("partner-1", any()) }
        coVerify(exactly = 1) {
            pushService.notifyUser(
                "partner-1",
                "Status update",
                "Partner is BUSY",
                mapOf("reason" to "realtime_error")
            )
        }
    }

    @Test
    fun `given SelfStatusChanged when dispatch then notifies self via realtime`() = runTest {
        // Given
        val event = NotificationEvent.SelfStatusChanged(
            userId = "user-1",
            status = SemaphoreStatus.AVAILABLE
        )
        coEvery {
            realtimeService.notifySelfStatusChanged("user-1", any<SelfStatusChangedEvent>())
        } returns true

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) {
            realtimeService.notifySelfStatusChanged(
                "user-1",
                match {
                    it.type == "SELF_STATUS_CHANGED" &&
                        it.userId == "user-1" &&
                        it.status == SemaphoreStatus.AVAILABLE &&
                        it.statusExpiration == null &&
                        it.timestamp > 0
                }
            )
        }
        coVerify(exactly = 0) { pushService.notifyUser(any(), any(), any(), any()) }
    }

    @Test
    fun `given partner online when PartnerUnlinked then sends realtime only`() = runTest {
        // Given
        val event = NotificationEvent.PartnerUnlinked(
            actorUserId = "sender-1",
            recipientUserId = "partner-1"
        )
        coEvery {
            realtimeService.notifyPartnerUnlinked("partner-1", any<PartnerUnlinkedEvent>())
        } returns true

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) {
            realtimeService.notifyPartnerUnlinked(
                "partner-1",
                match {
                    it.type == "PARTNER_UNLINKED" &&
                        it.partnerId == "sender-1" &&
                        it.timestamp > 0
                }
            )
        }
        coVerify(exactly = 0) { pushService.notifyUser(any(), any(), any(), any()) }
    }

    @Test
    fun `given partner offline when PartnerUnlinked then falls back to push`() = runTest {
        // Given
        val event = NotificationEvent.PartnerUnlinked(
            actorUserId = "sender-1",
            recipientUserId = "partner-1"
        )
        coEvery {
            realtimeService.notifyPartnerUnlinked("partner-1", any<PartnerUnlinkedEvent>())
        } returns false
        every { mapper.toPayload(event) } returns NotificationPayload(
            title = "Partner unlinked",
            body = "Your partner has been unlinked",
            data = mapOf("type" to "PARTNER_UNLINKED")
        )
        coEvery { pushService.notifyUser(any(), any(), any(), any()) } returns PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) {
            realtimeService.notifyPartnerUnlinked("partner-1", any())
        }
        coVerify(exactly = 1) {
            pushService.notifyUser(
                "partner-1",
                "Partner unlinked",
                "Your partner has been unlinked",
                mapOf("type" to "PARTNER_UNLINKED")
            )
        }
    }

    @Test
    fun `given push fallback with permanent and temporary failures when partner offline then logs results without error`() = runTest {
        // Given
        val event = NotificationEvent.PartnerStatusChanged(
            actorUserId = "sender-1",
            recipientUserId = "partner-1",
            status = SemaphoreStatus.BUSY
        )
        coEvery {
            realtimeService.notifyPartnerStatusChanged("partner-1", any<PartnerStatusChangedEvent>())
        } returns false
        every { mapper.toPayload(event) } returns NotificationPayload(
            title = "Status update",
            body = "Partner is BUSY",
            data = emptyMap()
        )
        val testResult = PushDispatchResult(
            totalTokens = 3,
            attempted = 3,
            delivered = 1,
            results = listOf(
                PushResult.Success("t1"),
                PushResult.PermanentFailure("t2", "unregistered", "UNREGISTERED"),
                PushResult.TemporaryFailure("t3", "unavailable", "UNAVAILABLE")
            )
        )
        coEvery { pushService.notifyUser(any(), any(), any(), any()) } returns testResult

        // When
        dispatcher.dispatch(event)

        // Then — no crash, push was called, result was captured and logged
        coVerify(exactly = 1) {
            pushService.notifyUser("partner-1", "Status update", "Partner is BUSY", emptyMap())
        }
    }

    @Test
    fun `given fallback to push when PartnerStatusChanged then uses mapper to convert event`() = runTest {
        // Given
        val event = NotificationEvent.PartnerStatusChanged(
            actorUserId = "sender-1",
            recipientUserId = "partner-1",
            status = SemaphoreStatus.BUSY
        )
        coEvery {
            realtimeService.notifyPartnerStatusChanged("partner-1", any<PartnerStatusChangedEvent>())
        } returns false
        every { mapper.toPayload(event) } returns NotificationPayload(
            title = "Partner status",
            body = "Partner changed to BUSY",
            data = mapOf("actorId" to "sender-1", "status" to "BUSY")
        )
        coEvery { pushService.notifyUser(any(), any(), any(), any()) } returns PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)

        // When
        dispatcher.dispatch(event)

        // Then
        coVerify(exactly = 1) { mapper.toPayload(event) }
        coVerify(exactly = 1) {
            pushService.notifyUser(
                "partner-1",
                "Partner status",
                "Partner changed to BUSY",
                mapOf("actorId" to "sender-1", "status" to "BUSY")
            )
        }
    }
}
