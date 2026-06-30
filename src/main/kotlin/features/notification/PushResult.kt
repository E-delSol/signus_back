package com.pecadoartesano.features.notification

/**
 * Per-token result from a push delivery attempt.
 *
 * @param token The FCM token this result corresponds to.
 */
sealed class PushResult {
    data class Success(val token: String) : PushResult()
    data class PermanentFailure(val token: String, val reason: String, val errorCode: String) : PushResult()
    data class TemporaryFailure(val token: String, val reason: String, val errorCode: String) : PushResult()
}
