package com.pecadoartesano.features.notification

import org.slf4j.LoggerFactory

/**
 * Classifies FCM error codes into permanent or temporary failure.
 *
 * Classification rules:
 * - **Permanent** (safe to deactivate): UNREGISTERED, INVALID_ARGUMENT
 * - **Temporary** (retryable): UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED
 * - **Unknown**: Defaults to TemporaryFailure (conservative — won't deactivate valid tokens)
 *
 * Matching is case-insensitive.
 */
object FcmErrorClassifier {

    private val logger = LoggerFactory.getLogger(FcmErrorClassifier::class.java)

    private val permanentErrors = setOf("UNREGISTERED", "INVALID_ARGUMENT")
    private val temporaryErrors = setOf("UNAVAILABLE", "INTERNAL", "QUOTA_EXCEEDED")

    /**
     * Classify an FCM error code for the given [token].
     *
     * @param token     The FCM token that produced the error.
     * @param errorCode The raw FCM error string (e.g., "UNREGISTERED", "Unavailable").
     * @return A [PushResult] reflecting the error classification.
     */
    fun classify(token: String, errorCode: String): PushResult {
        val upper = errorCode.uppercase()
        val lower = errorCode.lowercase()
        return when (upper) {
            in permanentErrors -> PushResult.PermanentFailure(
                token = token,
                reason = lower,
                errorCode = upper
            )
            in temporaryErrors -> PushResult.TemporaryFailure(
                token = token,
                reason = lower,
                errorCode = upper
            )
            else -> {
                logger.warn("Unknown FCM error code '{}' for token {}, mapping to TemporaryFailure", upper, token)
                PushResult.TemporaryFailure(
                    token = token,
                    reason = lower,
                    errorCode = upper
                )
            }
        }
    }
}
