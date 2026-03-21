package com.pecadoartesano.features.notification.dto

import kotlinx.serialization.Serializable

@Serializable
data class PartnerUnlinkedEvent(
    val type: String = "PARTNER_UNLINKED",
    val partnerId: String,
    val timestamp: Long
)
