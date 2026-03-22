package com.pecadoartesano.features.user.dto

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val id: String,
    val status: SemaphoreStatus?,
    val statusExpiration: Long?,
    val statusDuration: Long?,
    val partnerId: String?,
    val displayName: String?,
)
