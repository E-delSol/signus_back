package com.pecadoartesano.features.devicetoken.ports

import com.pecadoartesano.features.devicetoken.DeviceToken

interface DeviceTokenRepositoryPort {
    fun upsertForUserDevice(
        userId: String,
        deviceId: String,
        fcmToken: String,
        platform: String,
        appVersion: String?,
        nowMillis: Long
    ): DeviceTokenUpsertResult

    fun deactivateForUserDevice(userId: String, deviceId: String, nowMillis: Long): Boolean

    fun listByUserId(userId: String, includeInactive: Boolean): List<DeviceToken>
}
