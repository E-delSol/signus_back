package com.pecadoartesano.features.notification.ports

import com.pecadoartesano.features.notification.PushDispatchResult

interface PushNotificationService {
    suspend fun notifyUser(
        targetUserId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): PushDispatchResult
}
