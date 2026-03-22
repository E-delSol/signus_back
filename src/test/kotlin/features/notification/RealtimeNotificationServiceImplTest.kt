package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
import com.pecadoartesano.features.notification.dto.SelfStatusChangedEvent
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RealtimeNotificationServiceImplTest {

    @Test
    fun `given no session when notify then returns false`() = runTest {
        // Given
        val service = RealtimeNotificationServiceImpl()
        val event = PartnerStatusChangedEvent(
            partnerId = "sender-1",
            status = SemaphoreStatus.BUSY,
            statusExpiration = null,
            timestamp = 1_700_000_000_000
        )

        // When
        val delivered = service.notifyPartnerStatusChanged("partner-1", event)

        // Then
        assertFalse(delivered)
    }

    @Test
    fun `given registered session when notify then sends event and returns true`() = runTest {
        // Given
        val service = RealtimeNotificationServiceImpl()
        val session = mockk<WebSocketSession>()
        val frameSlot = io.mockk.slot<Frame>()
        coEvery { session.send(capture(frameSlot)) } returns Unit
        service.registerSession("partner-1", session)
        val event = PartnerStatusChangedEvent(
            partnerId = "sender-1",
            status = SemaphoreStatus.AVAILABLE,
            statusExpiration = null,
            timestamp = 1_700_000_000_000
        )

        // When
        val delivered = service.notifyPartnerStatusChanged("partner-1", event)

        // Then
        assertTrue(delivered)
        coVerify(exactly = 1) { session.send(any()) }
        val sent = frameSlot.captured as Frame.Text
        val text = sent.data.decodeToString()
        assertTrue(text.contains("\"partnerId\":\"sender-1\""))
        assertTrue(text.contains("\"status\":\"AVAILABLE\""))
        assertTrue(text.contains("\"statusExpiration\":null"))
    }

    @Test
    fun `given multiple sessions for same user when notify self status then sends event to all sessions`() = runTest {
        // Given
        val service = RealtimeNotificationServiceImpl()
        val firstSession = mockk<WebSocketSession>()
        val secondSession = mockk<WebSocketSession>()
        val firstFrameSlot = io.mockk.slot<Frame>()
        val secondFrameSlot = io.mockk.slot<Frame>()
        coEvery { firstSession.send(capture(firstFrameSlot)) } returns Unit
        coEvery { secondSession.send(capture(secondFrameSlot)) } returns Unit
        service.registerSession("user-1", firstSession)
        service.registerSession("user-1", secondSession)
        val event = SelfStatusChangedEvent(
            userId = "user-1",
            status = SemaphoreStatus.BUSY,
            statusExpiration = null,
            timestamp = 1_700_000_000_000
        )

        // When
        val delivered = service.notifySelfStatusChanged("user-1", event)

        // Then
        assertTrue(delivered)
        coVerify(exactly = 1) { firstSession.send(any()) }
        coVerify(exactly = 1) { secondSession.send(any()) }
        assertTrue((firstFrameSlot.captured as Frame.Text).data.decodeToString().contains("\"type\":\"SELF_STATUS_CHANGED\""))
        assertTrue((secondFrameSlot.captured as Frame.Text).data.decodeToString().contains("\"userId\":\"user-1\""))
    }

    @Test
    fun `given multiple sessions for same user when one is removed then keeps remaining session active`() = runTest {
        // Given
        val service = RealtimeNotificationServiceImpl()
        val removedSession = mockk<WebSocketSession>()
        val activeSession = mockk<WebSocketSession>()
        coEvery { activeSession.send(any()) } returns Unit
        service.registerSession("user-1", removedSession)
        service.registerSession("user-1", activeSession)
        service.removeSession("user-1", removedSession)
        val event = SelfStatusChangedEvent(
            userId = "user-1",
            status = SemaphoreStatus.AVAILABLE,
            statusExpiration = null,
            timestamp = 1_700_000_000_000
        )

        // When
        val delivered = service.notifySelfStatusChanged("user-1", event)

        // Then
        assertTrue(delivered)
        coVerify(exactly = 0) { removedSession.send(any()) }
        coVerify(exactly = 1) { activeSession.send(any()) }
    }

    @Test
    fun `given registered session when notify partner unlinked then sends event and returns true`() = runTest {
        // Given
        val service = RealtimeNotificationServiceImpl()
        val session = mockk<WebSocketSession>()
        val frameSlot = io.mockk.slot<Frame>()
        coEvery { session.send(capture(frameSlot)) } returns Unit
        service.registerSession("partner-1", session)
        val event = PartnerUnlinkedEvent(
            partnerId = "sender-1",
            timestamp = 1_700_000_000_000
        )

        // When
        val delivered = service.notifyPartnerUnlinked("partner-1", event)

        // Then
        assertTrue(delivered)
        coVerify(exactly = 1) { session.send(any()) }
        val sent = frameSlot.captured as Frame.Text
        val text = sent.data.decodeToString()
        assertTrue(text.contains("\"type\":\"PARTNER_UNLINKED\""))
        assertTrue(text.contains("\"partnerId\":\"sender-1\""))
    }
}
