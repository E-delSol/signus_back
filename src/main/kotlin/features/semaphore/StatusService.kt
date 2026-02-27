package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort

class StatusService(
    private val semaphoreRepository: SemaphoreRepositoryPort,
    private val notificationOrchestrator: NotificationOrchestrator
) {
    suspend fun updateStatus(senderId: String, newStatus: SemaphoreStatus): Semaphore {
        val updated = semaphoreRepository.updateUserStatus(senderId, newStatus)
        notificationOrchestrator.notifyPartnerAboutStatusChange(senderId, newStatus)
        return updated
    }
}
