package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
import com.pecadoartesano.features.notification.dto.SelfStatusChangedEvent
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RealtimeNotificationServiceImpl(
    private val json: Json = Json {
        encodeDefaults = true
    }
) : RealtimeNotificationService {
    private val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    override fun registerSession(userId: String, session: WebSocketSession) {
        sessions.compute(userId) { _, activeSessions ->
            (activeSessions ?: ConcurrentHashMap.newKeySet()).apply {
                add(session)
            }
        }
    }

    override fun removeSession(userId: String, session: WebSocketSession) {
        sessions.computeIfPresent(userId) { _, activeSessions ->
            activeSessions.remove(session)
            activeSessions.takeIf { it.isNotEmpty() }
        }
    }

    override suspend fun notifySelfStatusChanged(targetUserId: String, event: SelfStatusChangedEvent): Boolean {
        val payload = json.encodeToString(SelfStatusChangedEvent.serializer(), event)
        return sendToUser(targetUserId, payload)
    }

    override suspend fun notifyPartnerStatusChanged(targetUserId: String, event: PartnerStatusChangedEvent): Boolean {
        val payload = json.encodeToString(PartnerStatusChangedEvent.serializer(), event)
        return sendToUser(targetUserId, payload)
    }

    override suspend fun notifyPartnerUnlinked(targetUserId: String, event: PartnerUnlinkedEvent): Boolean {
        val payload = json.encodeToString(PartnerUnlinkedEvent.serializer(), event)
        return sendToUser(targetUserId, payload)
    }

    private suspend fun sendToUser(targetUserId: String, payload: String): Boolean {
        val activeSessions = sessions[targetUserId]?.toList().orEmpty()
        if (activeSessions.isEmpty()) {
            return false
        }

        var delivered = false

        activeSessions.forEach { session ->
            val sent = runCatching {
                session.send(Frame.Text(payload))
            }.isSuccess

            if (sent) {
                delivered = true
            } else {
                removeSession(targetUserId, session)
            }
        }

        return delivered
    }
}
