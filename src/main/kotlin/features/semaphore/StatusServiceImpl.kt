package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.semaphore.ports.StatusService
import com.pecadoartesano.features.semaphore.dto.SemaphoreStatusChangedEvent
import org.slf4j.LoggerFactory

class StatusServiceImpl(
    private val semaphoreRepository: SemaphoreRepositoryPort,
    private val partnerLookup: PartnerLookupPort,
    private val realtimeNotificationService: RealtimeNotificationService
) : StatusService {
    private val logger = LoggerFactory.getLogger(StatusServiceImpl::class.java)

    override suspend fun updateStatus(senderId: String, newStatus: SemaphoreStatus): Semaphore {
        val updated = semaphoreRepository.updateUserStatus(senderId, newStatus)

        val partner = partnerLookup.findPartnerByUserId(senderId)
        if (partner != null) {
            logger.info("Semaphore status changed for user {}, notifying partner {}", senderId, partner.id)
            realtimeNotificationService.notifySemaphoreStatusChanged(
                targetUserId = partner.id,
                event = SemaphoreStatusChangedEvent(
                    userId = senderId,
                    status = newStatus,
                    statusExpiration = updated.expiration,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        return updated
    }
}
