package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.PartnerUnlinkedEvent
import com.pecadoartesano.features.notification.dto.SelfStatusChangedEvent
import com.pecadoartesano.features.notification.ports.NotificationDispatcher
import com.pecadoartesano.features.notification.ports.NotificationMapper
import com.pecadoartesano.features.notification.ports.PushNotificationService
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import org.slf4j.LoggerFactory

class NotificationDispatcherImpl(
    private val realtimeService: RealtimeNotificationService,
    private val pushService: PushNotificationService,
    private val mapper: NotificationMapper
) : NotificationDispatcher {

    private val logger = LoggerFactory.getLogger(NotificationDispatcherImpl::class.java)

    override suspend fun dispatch(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.PartnerStatusChanged -> dispatchPartnerStatusChanged(event)
            is NotificationEvent.SelfStatusChanged -> dispatchSelfStatusChanged(event)
            is NotificationEvent.PartnerUnlinked -> dispatchPartnerUnlinked(event)
        }
    }

    private suspend fun dispatchPartnerStatusChanged(event: NotificationEvent.PartnerStatusChanged) {
        val timestamp = System.currentTimeMillis()

        val deliveredRealtime = runCatching {
            realtimeService.notifyPartnerStatusChanged(
                targetUserId = event.recipientUserId,
                event = PartnerStatusChangedEvent(
                    partnerId = event.actorUserId,
                    status = event.status,
                    statusExpiration = event.statusExpiration?.toEpochMilli(),
                    timestamp = timestamp
                )
            )
        }.onFailure { throwable ->
            logger.warn("Realtime notification failed for partner {}", event.recipientUserId, throwable)
        }.getOrDefault(false)

        if (!deliveredRealtime) {
            pushFallback(event.recipientUserId, event)
        }
    }

    private suspend fun dispatchSelfStatusChanged(event: NotificationEvent.SelfStatusChanged) {
        val timestamp = System.currentTimeMillis()

        runCatching {
            realtimeService.notifySelfStatusChanged(
                targetUserId = event.userId,
                event = SelfStatusChangedEvent(
                    userId = event.userId,
                    status = event.status,
                    statusExpiration = event.statusExpiration?.toEpochMilli(),
                    timestamp = timestamp
                )
            )
        }.onFailure { throwable ->
            logger.warn("Realtime self notification failed for user {}", event.userId, throwable)
        }
    }

    private suspend fun dispatchPartnerUnlinked(event: NotificationEvent.PartnerUnlinked) {
        val timestamp = System.currentTimeMillis()

        val deliveredRealtime = runCatching {
            realtimeService.notifyPartnerUnlinked(
                targetUserId = event.recipientUserId,
                event = PartnerUnlinkedEvent(
                    partnerId = event.actorUserId,
                    timestamp = timestamp
                )
            )
        }.onFailure { throwable ->
            logger.warn("Realtime unlinked notification failed for partner {}", event.recipientUserId, throwable)
        }.getOrDefault(false)

        if (!deliveredRealtime) {
            pushFallback(event.recipientUserId, event)
        }
    }

    private suspend fun pushFallback(targetUserId: String, event: NotificationEvent) {
        val payload = mapper.toPayload(event)
        val result = pushService.notifyUser(
            targetUserId = targetUserId,
            title = payload.title,
            body = payload.body,
            data = payload.data
        )

        if (result.permanentFailures.isNotEmpty()) {
            logger.warn(
                "Push fallback for user {}: {} permanent failures ({}). Deactivated tokens will not be retried.",
                targetUserId, result.permanentFailures.size,
                result.permanentFailures.joinToString(", ") { "${it.token}:${it.reason}" }
            )
        }
        if (result.temporaryFailures.isNotEmpty()) {
            logger.warn(
                "Push fallback for user {}: {} temporary failures ({}). Some tokens may not have received the notification.",
                targetUserId, result.temporaryFailures.size,
                result.temporaryFailures.joinToString(", ") { "${it.token}:${it.reason}" }
            )
        }
    }
}
