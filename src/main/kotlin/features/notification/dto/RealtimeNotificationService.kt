package com.pecadoartesano.features.notification.dto

import io.ktor.websocket.WebSocketSession

interface RealtimeNotificationService {
    fun registerSession(userId: String, session: WebSocketSession)
    fun removeSession(userId: String)
    suspend fun notifyPartnerStatusChanged(targetUserId: String, event: PartnerStatusChangedEvent): Boolean
}
