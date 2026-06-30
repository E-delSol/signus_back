package com.pecadoartesano.features.notification

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import java.time.Instant

sealed class NotificationEvent {
    data class PartnerStatusChanged(
        val actorUserId: String,
        val recipientUserId: String,
        val status: SemaphoreStatus,
        val statusExpiration: Instant? = null
    ) : NotificationEvent()

    data class SelfStatusChanged(
        val userId: String,
        val status: SemaphoreStatus,
        val statusExpiration: Instant? = null
    ) : NotificationEvent()

    data class PartnerUnlinked(
        val actorUserId: String,
        val recipientUserId: String
    ) : NotificationEvent()
}
