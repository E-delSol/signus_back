package com.pecadoartesano.features.notification.ports

import com.pecadoartesano.features.notification.NotificationEvent

interface NotificationMapper {
    fun toPayload(event: NotificationEvent): NotificationPayload
}

data class NotificationPayload(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)
