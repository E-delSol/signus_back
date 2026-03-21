package com.pecadoartesano.features.devicetoken.ports

import com.pecadoartesano.features.devicetoken.DeviceToken

interface DeviceTokenService {
    fun upsertForUser(
        userId: String,
        deviceId: String,
        fcmToken: String,
        platform: String,
        appVersion: String?
    ): DeviceTokenUpsertResult

    fun deactivateForUserDevice(userId: String, deviceId: String): Boolean

    fun listForUser(userId: String, includeInactive: Boolean = false): List<DeviceToken>
}

data class DeviceTokenUpsertResult(
    val token: DeviceToken,
    val created: Boolean
)
