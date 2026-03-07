package com.pecadoartesano.features.semaphore.dto

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import kotlinx.serialization.Serializable

@Serializable
data class StatusPatchRequest(
    val status: SemaphoreStatus
)
