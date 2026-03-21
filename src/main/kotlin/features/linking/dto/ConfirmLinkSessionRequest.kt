package com.pecadoartesano.features.linking.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConfirmLinkSessionRequest(
    val linkCode: String
)
