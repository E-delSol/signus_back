package com.pecadoartesano.features.semaphore.ports

import com.pecadoartesano.features.semaphore.Semaphore
import com.pecadoartesano.features.semaphore.SemaphoreStatus

interface StatusService {
    suspend fun updateStatus(senderId: String, newStatus: SemaphoreStatus): Semaphore
}
