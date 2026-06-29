package com.pecadoartesano.features.notification

/**
 * Aggregate result of a push dispatch to all tokens for a single user.
 *
 * @param totalTokens Total number of active tokens found for the user.
 * @param attempted Number of tokens that were actually attempted (should match totalTokens unless empty).
 * @param delivered Number of tokens that received a successful push.
 * @param results Per-token [PushResult] list, enabling downstream consumers to
 *                inspect specific failures and take action (e.g., deactivate on permanent failures).
 */
data class PushDispatchResult(
    val totalTokens: Int,
    val attempted: Int,
    val delivered: Int,
    val results: List<PushResult> = emptyList()
) {
    /** All permanent failures from this dispatch batch. */
    val permanentFailures: List<PushResult.PermanentFailure>
        get() = results.filterIsInstance<PushResult.PermanentFailure>()

    /** All temporary failures from this dispatch batch. */
    val temporaryFailures: List<PushResult.TemporaryFailure>
        get() = results.filterIsInstance<PushResult.TemporaryFailure>()

    /** True if no temporary failures remain (all permanent or success). */
    val isCompleted: Boolean
        get() = results.none { it is PushResult.TemporaryFailure }
}
