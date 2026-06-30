package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationEvent
import com.pecadoartesano.features.notification.ports.NotificationDispatcher
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.semaphore.ports.StatusService
import org.slf4j.LoggerFactory

class StatusServiceImpl(
    private val semaphoreRepository: SemaphoreRepositoryPort,
    private val notificationDispatcher: NotificationDispatcher,
    private val partnerLookup: PartnerLookupPort
) : StatusService {
    private val logger = LoggerFactory.getLogger(StatusServiceImpl::class.java)

    override suspend fun updateStatus(senderId: String, newStatus: SemaphoreStatus): Semaphore {
        val updated = semaphoreRepository.updateUserStatus(senderId, newStatus)

        runCatching {
            notificationDispatcher.dispatch(
                NotificationEvent.SelfStatusChanged(
                    userId = senderId,
                    status = newStatus
                )
            )
        }.onFailure { throwable ->
            logger.warn("Status updated for user {} but self notification dispatch failed", senderId, throwable)
        }

        runCatching {
            val partner = partnerLookup.findPartnerByUserId(senderId)
            if (partner != null) {
                notificationDispatcher.dispatch(
                    NotificationEvent.PartnerStatusChanged(
                        actorUserId = senderId,
                        recipientUserId = partner.id,
                        status = newStatus
                    )
                )
            }
        }.onFailure { throwable ->
            logger.warn("Status updated for user {} but partner notification dispatch failed", senderId, throwable)
        }

        return updated
    }
}
