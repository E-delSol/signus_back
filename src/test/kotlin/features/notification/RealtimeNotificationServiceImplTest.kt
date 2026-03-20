package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
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
