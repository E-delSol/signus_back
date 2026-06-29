package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.ports.DeviceTokenLookupPort
import com.pecadoartesano.features.notification.ports.PushNotificationService
import org.slf4j.LoggerFactory

class PartnerPushNotificationService(
    private val deviceTokenLookup: DeviceTokenLookupPort,
    private val pushProvider: PushProvider
) : PushNotificationService {
    private val logger = LoggerFactory.getLogger(PartnerPushNotificationService::class.java)

    override suspend fun notifyUser(
        targetUserId: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): PushDispatchResult {
        val activeTokens = deviceTokenLookup.findActiveFcmTokensByUserId(targetUserId)
        if (activeTokens.isEmpty()) {
            logger.info("No active FCM tokens found for user {}", targetUserId)
            return PushDispatchResult(totalTokens = 0, attempted = 0, delivered = 0)
        }

        var delivered = 0
        activeTokens.forEach { token ->
            val result = runCatching {
                pushProvider.sendPush(
                    targetUserId = targetUserId,
                    token = token,
                    title = title,
                    body = body
                )
            }.onFailure { throwable ->
                logger.warn("Push send failed for user {} token {}", targetUserId, token, throwable)
            }.getOrDefault(
                PushResult.TemporaryFailure(token, "exception", "UNKNOWN")
            )

            if (result is PushResult.Success) {
                delivered++
            }
            // Phase 3 will add deactivation logic for PermanentFailure
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
