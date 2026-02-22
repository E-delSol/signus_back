package com.pecadoartesano.features.notification.dto

import com.pecadoartesano.features.semaphore.SemaphoreStatus
import kotlinx.serialization.Serializable

@Serializable
data class PartnerStatusChangedEvent(
    val type: String = "PARTNER_STATUS_CHANGED",
    val senderId: String,
    val partnerId: String,
    val status: SemaphoreStatus
)
