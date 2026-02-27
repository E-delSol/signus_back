package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
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
            senderId = "sender-1",
            partnerId = "partner-1",
            status = SemaphoreStatus.BUSY
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
            senderId = "sender-1",
            partnerId = "partner-1",
            status = SemaphoreStatus.AVAILABLE
        )

        // When
        val delivered = service.notifyPartnerStatusChanged("partner-1", event)

        // Then
        assertTrue(delivered)
        coVerify(exactly = 1) { session.send(any()) }
        val sent = frameSlot.captured as Frame.Text
        val text = sent.data.decodeToString()
        assertTrue(text.contains("\"partnerId\":\"partner-1\""))
        assertTrue(text.contains("\"status\":\"AVAILABLE\""))
    }
}
