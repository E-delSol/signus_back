package com.pecadoartesano.features.devicetoken.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpsertDeviceTokenResponse(
    val created: Boolean,
    val token: DeviceTokenResponse
)
