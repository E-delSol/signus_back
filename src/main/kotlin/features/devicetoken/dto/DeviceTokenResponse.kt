package com.pecadoartesano.features.devicetoken.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenResponse(
    val id: String,
    val deviceId: String,
    val platform: String,
    val appVersion: String?,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastRegisteredAt: Long,
    val deactivatedAt: Long?
)
