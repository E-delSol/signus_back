package com.pecadoartesano.features.notification.dto

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import kotlinx.serialization.Serializable

@Serializable
data class PartnerStatusChangedEvent(
    val type: String = "PARTNER_STATUS_CHANGED",
    val partnerId: String,
    val status: SemaphoreStatus,
    val statusExpiration: Long?,
    val timestamp: Long
)
