package com.pecadoartesano.features.devicetoken

data class DeviceToken(
    val id: String,
    val userId: String,
    val deviceId: String,
    val fcmToken: String,
    val platform: String,
    val appVersion: String?,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastRegisteredAt: Long,
    val deactivatedAt: Long? = null
)
