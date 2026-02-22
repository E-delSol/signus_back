package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.user.UserRepository

class NotificationOrchestrator(
    private val userRepository: UserRepository,
    private val realtimeNotificationService: RealtimeNotificationService,
    private val pushProvider: PushProvider
) {
    suspend fun notifyPartnerAboutStatusChange(senderId: String, newStatus: SemaphoreStatus) {
        val partner = userRepository.findPartnerByUserId(senderId) ?: return

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
