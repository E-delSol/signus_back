package com.pecadoartesano.features.notification.ports

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
import com.pecadoartesano.features.notification.dto.SelfStatusChangedEvent

import io.ktor.websocket.WebSocketSession

interface RealtimeNotificationService {
    fun registerSession(userId: String, session: WebSocketSession)
    fun removeSession(userId: String, session: WebSocketSession)
    suspend fun notifySelfStatusChanged(targetUserId: String, event: SelfStatusChangedEvent): Boolean
    suspend fun notifyPartnerStatusChanged(targetUserId: String, event: PartnerStatusChangedEvent): Boolean
    suspend fun notifyPartnerUnlinked(targetUserId: String, event: PartnerUnlinkedEvent): Boolean
}
