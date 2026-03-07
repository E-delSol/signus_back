package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.dto.SemaphoreStatusChangedEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RealtimeNotificationServiceImpl(
    private val json: Json = Json
) : RealtimeNotificationService {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun registerSession(userId: String, session: WebSocketSession) {
        sessions[userId] = session
    }

    override fun removeSession(userId: String) {
        sessions.remove(userId)
    }

    override suspend fun notifyPartnerStatusChanged(targetUserId: String, event: PartnerStatusChangedEvent): Boolean {
        val payload = json.encodeToString(PartnerStatusChangedEvent.serializer(), event)
        return sendToUser(targetUserId, payload)
    }

    override suspend fun notifySemaphoreStatusChanged(targetUserId: String, event: SemaphoreStatusChangedEvent): Boolean {
        val payload = json.encodeToString(SemaphoreStatusChangedEvent.serializer(), event)
        return sendToUser(targetUserId, payload)
    }

    private suspend fun sendToUser(targetUserId: String, payload: String): Boolean {
        val session = sessions[targetUserId] ?: return false
        return runCatching {
            session.send(Frame.Text(payload))
        }.isSuccess
    }
}
