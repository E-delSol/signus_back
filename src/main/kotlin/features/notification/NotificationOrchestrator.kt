package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.notification.dto.RealtimeNotificationService
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.semaphore.SemaphoreStatus

class NotificationOrchestrator(
    private val partnerLookup: PartnerLookupPort,
    private val realtimeNotificationService: RealtimeNotificationService,
    private val pushProvider: PushProvider
) {
    suspend fun notifyPartnerAboutStatusChange(senderId: String, newStatus: SemaphoreStatus) {
        val partner = partnerLookup.findPartnerByUserId(senderId) ?: return

        val event = PartnerStatusChangedEvent(
            senderId = senderId,
            partnerId = partner.id,
            status = newStatus
        )

        val deliveredRealtime = realtimeNotificationService.notifyPartnerStatusChanged(partner.id, event)

        if (!deliveredRealtime && !partner.fcmToken.isNullOrBlank()) {
            pushProvider.sendPush(
                targetUserId = partner.id,
                token = partner.fcmToken,
                title = "Estado actualizado",
                body = "Tu pareja ahora está ${newStatus.name}"
            )
        }
    }
}
