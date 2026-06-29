package com.pecadoartesano.features.notification

import com.pecadoartesano.features.notification.providers.FcmResponse
import com.pecadoartesano.features.notification.providers.FcmResult
import com.pecadoartesano.features.notification.providers.checkHttpStatus
import com.pecadoartesano.features.notification.providers.parseFcmResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FcmPushProviderTest {

    // ── parseFcmResponse tests ────────────────────────────────────────────

    @Test
    fun `given success result when parse then returns Success`() {
        val response = FcmResponse(
            success = 1, failure = 0,
            results = listOf(FcmResult(error = null))
        )
        val result = parseFcmResponse("token-1", response)
        assertIs<PushResult.Success>(result)
        assertEquals("token-1", result.token)
    }

    @Test
    fun `given UNREGISTERED error when parse then returns PermanentFailure`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "UNREGISTERED"))
        )
        val result = parseFcmResponse("token-2", response)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("token-2", result.token)
        assertEquals("unregistered", result.reason)
        assertEquals("UNREGISTERED", result.errorCode)
    }

    @Test
    fun `given INVALID_ARGUMENT error when parse then returns PermanentFailure`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "INVALID_ARGUMENT"))
        )
        val result = parseFcmResponse("token-3", response)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("invalid_argument", result.reason)
    }

    @Test
    fun `given UNAVAILABLE error when parse then returns TemporaryFailure`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "UNAVAILABLE"))
        )
        val result = parseFcmResponse("token-4", response)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("unavailable", result.reason)
    }

    @Test
    fun `given INTERNAL error when parse then returns TemporaryFailure`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "INTERNAL"))
        )
        val result = parseFcmResponse("token-5", response)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("internal", result.reason)
    }

    @Test
    fun `given QUOTA_EXCEEDED error when parse then returns TemporaryFailure`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "QUOTA_EXCEEDED"))
        )
        val result = parseFcmResponse("token-6", response)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("quota_exceeded", result.reason)
    }

    @Test
    fun `given unknown error when parse then returns TemporaryFailure`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "SOME_UNKNOWN_ERROR"))
        )
        val result = parseFcmResponse("token-7", response)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("some_unknown_error", result.reason)
    }

    @Test
    fun `given empty results array when parse then returns PermanentFailure`() {
        val response = FcmResponse(success = 0, failure = 0, results = emptyList())
        val result = parseFcmResponse("token-8", response)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("empty_results", result.reason)
    }

    @Test
    fun `given lowercase error when parse then classifies case-insensitively`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "unregistered"))
        )
        val result = parseFcmResponse("token-9", response)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("UNREGISTERED", result.errorCode)
    }

    @Test
    fun `given mixed case error when parse then classifies case-insensitively`() {
        val response = FcmResponse(
            success = 0, failure = 1,
            results = listOf(FcmResult(error = "Unavailable"))
        )
        val result = parseFcmResponse("token-10", response)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("UNAVAILABLE", result.errorCode)
    }

    // ── checkHttpStatus tests ─────────────────────────────────────────────

    @Test
    fun `given http 200 when check status then returns null`() {
        val result = checkHttpStatus("token", 200)
        assertNull(result)
    }

    @Test
    fun `given http 500 when check status then returns TemporaryFailure`() {
        val result = checkHttpStatus("token-11", 500)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("http_error_500", result.reason)
    }

    @Test
    fun `given http 401 when check status then returns TemporaryFailure`() {
        val result = checkHttpStatus("token-12", 401)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("http_error_401", result.reason)
    }

    @Test
    fun `given http 403 when check status then returns TemporaryFailure`() {
        val result = checkHttpStatus("token-13", 403)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("http_error_403", result.reason)
    }
}
