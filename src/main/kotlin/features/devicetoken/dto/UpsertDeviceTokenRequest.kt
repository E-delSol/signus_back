package com.pecadoartesano.features.devicetoken.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpsertDeviceTokenRequest(
    val deviceId: String,
    val fcmToken: String,
    val platform: String,
    val appVersion: String? = null
)
