package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.semaphore.ports.StatusService

class StatusServiceImpl(
    private val semaphoreRepository: SemaphoreRepositoryPort,
    private val notificationOrchestrator: NotificationOrchestrator
) : StatusService {
    override suspend fun updateStatus(senderId: String, newStatus: SemaphoreStatus): Semaphore {
        val updated = semaphoreRepository.updateUserStatus(senderId, newStatus)
        notificationOrchestrator.notifyPartnerAboutStatusChange(senderId, newStatus)
        return updated
    }
}
