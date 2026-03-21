package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenService
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenUpsertResult

class DeviceTokenServiceImpl(
    private val repository: DeviceTokenRepositoryPort
) : DeviceTokenService {

    companion object {
        private const val MAX_DEVICE_ID_LENGTH = 191
        private const val MAX_FCM_TOKEN_LENGTH = 512
        private const val MAX_APP_VERSION_LENGTH = 64
        private const val ANDROID_PLATFORM = "android"
    }

    override fun upsertForUser(
        userId: String,
        deviceId: String,
        fcmToken: String,
        platform: String,
        appVersion: String?
    ): DeviceTokenUpsertResult {
        val normalizedUserId = userId.trim()
        val normalizedDeviceId = deviceId.trim()
        val normalizedToken = fcmToken.trim()
        val normalizedPlatform = platform.trim().lowercase()
        val normalizedAppVersion = appVersion?.trim()?.ifBlank { null }

        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedDeviceId.isNotBlank()) { "deviceId must not be blank" }
        require(normalizedDeviceId.length <= MAX_DEVICE_ID_LENGTH) {
            "deviceId length must be <= $MAX_DEVICE_ID_LENGTH"
        }
        require(normalizedToken.isNotBlank()) { "fcmToken must not be blank" }
        require(normalizedToken.length <= MAX_FCM_TOKEN_LENGTH) {
            "fcmToken length must be <= $MAX_FCM_TOKEN_LENGTH"
        }
        require(normalizedPlatform == ANDROID_PLATFORM) {
            "Unsupported platform. Allowed: $ANDROID_PLATFORM"
        }
        require(normalizedAppVersion == null || normalizedAppVersion.length <= MAX_APP_VERSION_LENGTH) {
            "appVersion length must be <= $MAX_APP_VERSION_LENGTH"
        }

        return repository.upsertForUserDevice(
            userId = normalizedUserId,
            deviceId = normalizedDeviceId,
            fcmToken = normalizedToken,
            platform = normalizedPlatform,
            appVersion = normalizedAppVersion,
            nowMillis = System.currentTimeMillis()
        )
    }

    override fun deactivateForUserDevice(userId: String, deviceId: String): Boolean {
        val normalizedUserId = userId.trim()
        val normalizedDeviceId = deviceId.trim()

        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        require(normalizedDeviceId.isNotBlank()) { "deviceId must not be blank" }
        require(normalizedDeviceId.length <= MAX_DEVICE_ID_LENGTH) {
            "deviceId length must be <= $MAX_DEVICE_ID_LENGTH"
        }

        return repository.deactivateForUserDevice(
            userId = normalizedUserId,
            deviceId = normalizedDeviceId,
            nowMillis = System.currentTimeMillis()
        )
    }

    override fun listForUser(userId: String, includeInactive: Boolean): List<DeviceToken> {
        val normalizedUserId = userId.trim()
        require(normalizedUserId.isNotBlank()) { "userId must not be blank" }
        return repository.listByUserId(normalizedUserId, includeInactive)
    }
}
