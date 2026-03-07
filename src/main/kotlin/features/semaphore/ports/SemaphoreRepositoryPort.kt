package com.pecadoartesano.features.semaphore.ports

import com.pecadoartesano.features.semaphore.Semaphore
import com.pecadoartesano.features.semaphore.SemaphoreStatus

interface SemaphoreRepositoryPort {
    fun updateUserStatus(userId: String, status: SemaphoreStatus): Semaphore
    fun findByUserId(userId: String): Semaphore?
}
