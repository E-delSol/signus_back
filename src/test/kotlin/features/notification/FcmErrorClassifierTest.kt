package com.pecadoartesano.features.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FcmErrorClassifierTest {

    @Test
    fun `given UNREGISTERED when classify then returns PermanentFailure`() {
        val result = FcmErrorClassifier.classify("tok-1", "UNREGISTERED")
        assertTrue(result is PushResult.PermanentFailure)
        result as PushResult.PermanentFailure
        assertEquals("tok-1", result.token)
        assertEquals("unregistered", result.reason)
        assertEquals("UNREGISTERED", result.errorCode)
    }

    @Test
    fun `given INVALID_ARGUMENT when classify then returns PermanentFailure`() {
        val result = FcmErrorClassifier.classify("tok-2", "INVALID_ARGUMENT")
        assertTrue(result is PushResult.PermanentFailure)
        result as PushResult.PermanentFailure
        assertEquals("tok-2", result.token)
        assertEquals("invalid_argument", result.reason)
        assertEquals("INVALID_ARGUMENT", result.errorCode)
    }

    @Test
    fun `given UNAVAILABLE when classify then returns TemporaryFailure`() {
        val result = FcmErrorClassifier.classify("tok-3", "UNAVAILABLE")
        assertTrue(result is PushResult.TemporaryFailure)
        result as PushResult.TemporaryFailure
        assertEquals("tok-3", result.token)
        assertEquals("unavailable", result.reason)
        assertEquals("UNAVAILABLE", result.errorCode)
    }

    @Test
    fun `given INTERNAL when classify then returns TemporaryFailure`() {
        val result = FcmErrorClassifier.classify("tok-4", "INTERNAL")
        assertTrue(result is PushResult.TemporaryFailure)
        assertEquals("internal", (result as PushResult.TemporaryFailure).reason)
        assertEquals("INTERNAL", result.errorCode)
    }

    @Test
    fun `given QUOTA_EXCEEDED when classify then returns TemporaryFailure`() {
        val result = FcmErrorClassifier.classify("tok-5", "QUOTA_EXCEEDED")
        assertTrue(result is PushResult.TemporaryFailure)
        assertEquals("quota_exceeded", (result as PushResult.TemporaryFailure).reason)
        assertEquals("QUOTA_EXCEEDED", result.errorCode)
    }

    @Test
    fun `given unknown error code when classify then returns TemporaryFailure`() {
        val result = FcmErrorClassifier.classify("tok-6", "SOME_UNKNOWN_ERROR")
        assertTrue(result is PushResult.TemporaryFailure)
        assertEquals("some_unknown_error", (result as PushResult.TemporaryFailure).reason)
        assertEquals("SOME_UNKNOWN_ERROR", result.errorCode)
    }

    @Test
    fun `given lowercase error code when classify then matches case-insensitively`() {
        val result = FcmErrorClassifier.classify("tok-7", "unregistered")
        assertTrue(result is PushResult.PermanentFailure)
        assertEquals("unregistered", (result as PushResult.PermanentFailure).reason)
        assertEquals("UNREGISTERED", result.errorCode)
    }

    @Test
    fun `given mixed case error code when classify then matches case-insensitively`() {
        val result = FcmErrorClassifier.classify("tok-8", "Unavailable")
        assertTrue(result is PushResult.TemporaryFailure)
        assertEquals("unavailable", (result as PushResult.TemporaryFailure).reason)
        assertEquals("UNAVAILABLE", result.errorCode)
    }
}
