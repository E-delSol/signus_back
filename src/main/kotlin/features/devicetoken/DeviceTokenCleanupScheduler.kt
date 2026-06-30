package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.core.config.TokenCleanupConfig
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DeviceTokenCleanupScheduler(
    private val repository: DeviceTokenRepositoryPort,
    private val config: TokenCleanupConfig,
    private val checkIntervalMs: Long = 24 * 60 * 60 * 1000L
) {
    private val logger = LoggerFactory.getLogger(DeviceTokenCleanupScheduler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                try {
                    runCleanup()
                } catch (e: Exception) {
                    logger.error("Token cleanup iteration failed", e)
                }
                if (isActive) {
                    delay(checkIntervalMs)
                }
            }
        }
    }

    suspend fun runCleanup() {
        try {
            val staleThreshold = System.currentTimeMillis() - (config.staleDays * 24L * 60L * 60L * 1000L)
            val staleTokens = repository.findActiveTokensOlderThan(staleThreshold)
            var deactivatedCount = 0
            for (token in staleTokens) {
                try {
                    repository.deactivateByFcmToken(token.fcmToken, "stale")
                    deactivatedCount++
                } catch (e: Exception) {
                    logger.warn("Failed to deactivate token ${token.fcmToken}", e)
                }
            }
            if (deactivatedCount > 0) {
                logger.info("Token cleanup: deactivated {} stale token(s)", deactivatedCount)
            }
        } catch (e: Exception) {
            logger.error("Token cleanup failed", e)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
