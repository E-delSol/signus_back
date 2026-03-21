package com.pecadoartesano.features.linking.dto

import com.pecadoartesano.features.linking.LinkSessionStatus
import kotlinx.serialization.Serializable

@Serializable
data class LinkSessionStatusResponse(
    val sessionId: String,
    val status: LinkSessionStatus
)
