package com.pecadoartesano.features.semaphore.dto

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import kotlinx.serialization.Serializable

@Serializable
data class SemaphoreStatusChangedEvent(
    val userId: String,
    val status: SemaphoreStatus,
    val statusExpiration: Long?,
    val timestamp: Long
)
