package com.pecadoartesano.features.notification.dto

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import kotlinx.serialization.Serializable

@Serializable
data class SelfStatusChangedEvent(
    val type: String = "SELF_STATUS_CHANGED",
    val userId: String,
    val status: SemaphoreStatus,
    val statusExpiration: Long?,
    val timestamp: Long
)
