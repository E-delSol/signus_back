package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.semaphore.ports.StatusService
import org.slf4j.LoggerFactory

class StatusServiceImpl(
    private val semaphoreRepository: SemaphoreRepositoryPort,
    private val notificationOrchestrator: NotificationOrchestrator
) : StatusService {
    private val logger = LoggerFactory.getLogger(StatusServiceImpl::class.java)

    override suspend fun updateStatus(senderId: String, newStatus: SemaphoreStatus): Semaphore {
        val updated = semaphoreRepository.updateUserStatus(senderId, newStatus)

        runCatching {
            notificationOrchestrator.notifyPartnerAboutStatusChange(senderId, newStatus)
        }.onFailure { throwable ->
            logger.warn("Status updated for user {} but notification dispatch failed", senderId, throwable)
        }

        return updated
    }
}
