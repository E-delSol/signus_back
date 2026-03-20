package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.ports.DeviceTokenLookupPort
import org.slf4j.LoggerFactory

class PartnerPushNotificationService(
    private val deviceTokenLookup: DeviceTokenLookupPort,
    private val pushProvider: PushProvider
) {
    private val logger = LoggerFactory.getLogger(PartnerPushNotificationService::class.java)

    suspend fun notifyUserDevices(targetUserId: String, title: String, body: String): PushDispatchResult {
        val activeTokens = deviceTokenLookup.findActiveFcmTokensByUserId(targetUserId)
        if (activeTokens.isEmpty()) {
            logger.info("No active FCM tokens found for user {}", targetUserId)
            return PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)
        }

        var delivered = 0
        activeTokens.forEach { token ->
            val sent = runCatching {
                pushProvider.sendPush(
                    targetUserId = targetUserId,
                    token = token,
                    title = title,
                    body = body
                )
            }.onFailure { throwable ->
                logger.warn("Push send failed for user {} token {}", targetUserId, token, throwable)
            }.getOrDefault(false)

            if (sent) {
                delivered++
            }
        }

        return PushDispatchResult(
            totalTokens = activeTokens.size,
            attempted = activeTokens.size,
            delivered = delivered
        )
    }
}

data class PushDispatchResult(
    val totalTokens: Int,
    val attempted: Int,
    val delivered: Int
)
