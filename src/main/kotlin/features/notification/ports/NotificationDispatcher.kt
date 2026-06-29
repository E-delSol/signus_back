package com.pecadoartesano.features.notification.ports

import com.pecadoartesano.features.notification.NotificationEvent

interface NotificationDispatcher {
    suspend fun dispatch(event: NotificationEvent)
}
