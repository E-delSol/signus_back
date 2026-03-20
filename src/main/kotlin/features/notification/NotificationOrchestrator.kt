package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import org.slf4j.LoggerFactory

class NotificationOrchestrator(
    private val partnerLookup: PartnerLookupPort,
    private val realtimeNotificationService: RealtimeNotificationService,
    private val partnerPushNotificationService: PartnerPushNotificationService
) {
    private val logger = LoggerFactory.getLogger(NotificationOrchestrator::class.java)

    suspend fun notifyPartnerAboutStatusChange(senderId: String, newStatus: SemaphoreStatus) {
        val partner = partnerLookup.findPartnerByUserId(senderId) ?: return

        val event = PartnerStatusChangedEvent(
            partnerId = senderId,
            status = newStatus,
            statusExpiration = null,
            timestamp = System.currentTimeMillis()
        )

        val deliveredRealtime = runCatching {
            realtimeNotificationService.notifyPartnerStatusChanged(partner.id, event)
        }.onFailure { throwable ->
            logger.warn("Realtime notification failed for partner {}", partner.id, throwable)
        }.getOrDefault(false)

        if (!deliveredRealtime) {
            partnerPushNotificationService.notifyUserDevices(
                targetUserId = partner.id,
                title = "Estado actualizado",
                body = "Tu pareja ahora está ${newStatus.name}"
            )
        }
    }
}
