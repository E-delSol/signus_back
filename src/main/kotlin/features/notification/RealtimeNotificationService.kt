package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RealtimeNotificationService(
    private val json: Json = Json
) {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun registerSession(userId: String, session: WebSocketSession) {
        sessions[userId] = session
    }

    fun removeSession(userId: String) {
        sessions.remove(userId)
    }

    suspend fun notifyPartnerStatusChanged(targetUserId: String, event: PartnerStatusChangedEvent): Boolean {
        val session = sessions[targetUserId] ?: return false
        return runCatching {
            session.send(Frame.Text(json.encodeToString(PartnerStatusChangedEvent.serializer(), event)))
        }.isSuccess
    }
}
