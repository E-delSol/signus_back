package com.pecadoartesano.features.notification

import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import com.pecadoartesano.features.notification.ports.DeviceTokenLookupPort
import com.pecadoartesano.features.notification.ports.PushNotificationService
import org.slf4j.LoggerFactory

class PartnerPushNotificationService(
    private val deviceTokenLookup: DeviceTokenLookupPort,
    private val pushProvider: PushProvider,
    private val deviceTokenRepository: DeviceTokenRepositoryPort
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

        val results = mutableListOf<PushResult>()
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

            results.add(result)

            when (result) {
                is PushResult.Success -> {
                    delivered++
                }
                is PushResult.PermanentFailure -> {
                    logger.info(
                        "Deactivating token {} for user {}: {} ({})",
                        result.token, targetUserId, result.reason, result.errorCode
                    )
                    deviceTokenRepository.deactivateByFcmToken(result.token, result.reason)
                }
                is PushResult.TemporaryFailure -> {
                    logger.warn(
                        "Temporary push failure for user {} token {}: {} ({})",
                        targetUserId, result.token, result.reason, result.errorCode
                    )
                }
            }
        }

        return PushDispatchResult(
            totalTokens = activeTokens.size,
            attempted = activeTokens.size,
            delivered = delivered,
            results = results
        )
    }
}
