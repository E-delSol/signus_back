package com.pecadoartesano.features.notification

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.pecadoartesano.features.notification.providers.FcmPushProvider
import com.pecadoartesano.features.notification.providers.FcmV1Error
import com.pecadoartesano.features.notification.providers.FcmV1Response
import com.pecadoartesano.features.notification.providers.parseFcmV1ResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

private val testServiceAccountJson = """{"type":"service_account","project_id":"test-project","private_key_id":"test","private_key":"-----BEGIN PRIVATE KEY-----\nMIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEA\n-----END PRIVATE KEY-----\n","client_email":"test@test.iam.gserviceaccount.com","client_id":"123","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"https://www.googleapis.com/robot/v1/metadata/x509/test@test.iam.gserviceaccount.com"}"""

class FcmPushProviderTest {

    // ── parseFcmV1ResponseBody unit tests ──────────────────────────────────

    @Test
    fun `given v1 200 with name when parse body then returns Success`() {
        val body = FcmV1Response(name = "projects/test/messages/msg123")
        val result = parseFcmV1ResponseBody("token-1", 200, body)
        assertIs<PushResult.Success>(result)
        assertEquals("token-1", result.token)
    }

    @Test
    fun `given v1 200 with missing name when parse body then returns PermanentFailure`() {
        val body = FcmV1Response(name = null)
        val result = parseFcmV1ResponseBody("token-2", 200, body)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("parse_error", result.reason)
    }

    @Test
    fun `given v1 200 with null body when parse then returns PermanentFailure`() {
        val result = parseFcmV1ResponseBody("token-3", 200, null)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("parse_error", result.reason)
    }

    @Test
    fun `given v1 UNREGISTERED error when parse body then returns PermanentFailure`() {
        val body = FcmV1Response(error = FcmV1Error(status = "UNREGISTERED"))
        val result = parseFcmV1ResponseBody("token-4", 404, body)
        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("unregistered", result.reason)
        assertEquals("UNREGISTERED", result.errorCode)
    }

    @Test
    fun `given v1 UNAVAILABLE error when parse body then returns TemporaryFailure`() {
        val body = FcmV1Response(error = FcmV1Error(status = "UNAVAILABLE"))
        val result = parseFcmV1ResponseBody("token-5", 503, body)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("unavailable", result.reason)
    }

    @Test
    fun `given v1 INTERNAL error when parse body then returns TemporaryFailure`() {
        val body = FcmV1Response(error = FcmV1Error(status = "INTERNAL"))
        val result = parseFcmV1ResponseBody("token-6", 500, body)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("internal", result.reason)
    }

    @Test
    fun `given v1 unknown error status when parse body then returns TemporaryFailure`() {
        val body = FcmV1Response(error = FcmV1Error(status = "SOME_UNKNOWN_CODE"))
        val result = parseFcmV1ResponseBody("token-7", 400, body)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("some_unknown_code", result.reason)
    }

    @Test
    fun `given v1 http error with unparseable body when parse then returns TemporaryFailure`() {
        val result = parseFcmV1ResponseBody("token-8", 503, null)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("http_error_503", result.reason)
    }

    @Test
    fun `given v1 error with no status field when parse then returns TemporaryFailure`() {
        val body = FcmV1Response(error = FcmV1Error(status = null))
        val result = parseFcmV1ResponseBody("token-9", 400, body)
        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("http_error_400", result.reason)
    }

    // ── FcmPushProvider.sendPush integration tests ─────────────────────────

    @Test
    fun `given v1 200 with name when sendPush then returns Success`() = runTest {
        val mockCredentials = mockk<GoogleCredentials>()
        every { mockCredentials.refreshIfExpired() } returns Unit
        every { mockCredentials.getAccessToken() } returns AccessToken("test-token", null)

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"name":"projects/test/messages/msg123"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val provider = FcmPushProvider(testServiceAccountJson, mockCredentials, client)
        val result = provider.sendPush("user-1", "token-1", "Title", "Body")

        assertIs<PushResult.Success>(result)
        assertEquals("token-1", result.token)
    }

    @Test
    fun `given v1 200 with missing name when sendPush then returns PermanentFailure`() = runTest {
        val mockCredentials = mockk<GoogleCredentials>()
        every { mockCredentials.refreshIfExpired() } returns Unit
        every { mockCredentials.getAccessToken() } returns AccessToken("test-token", null)

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("{}"),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val provider = FcmPushProvider(testServiceAccountJson, mockCredentials, client)
        val result = provider.sendPush("user-2", "token-2", "Title", "Body")

        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("parse_error", result.reason)
    }

    @Test
    fun `given v1 404 with UNREGISTERED when sendPush then returns PermanentFailure`() = runTest {
        val mockCredentials = mockk<GoogleCredentials>()
        every { mockCredentials.refreshIfExpired() } returns Unit
        every { mockCredentials.getAccessToken() } returns AccessToken("test-token", null)

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"error":{"status":"UNREGISTERED"}}"""),
                status = HttpStatusCode.NotFound,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val provider = FcmPushProvider(testServiceAccountJson, mockCredentials, client)
        val result = provider.sendPush("user-3", "token-3", "Title", "Body")

        assertIs<PushResult.PermanentFailure>(result)
        assertEquals("unregistered", result.reason)
    }

    @Test
    fun `given v1 503 with UNAVAILABLE when sendPush then returns TemporaryFailure`() = runTest {
        val mockCredentials = mockk<GoogleCredentials>()
        every { mockCredentials.refreshIfExpired() } returns Unit
        every { mockCredentials.getAccessToken() } returns AccessToken("test-token", null)

        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"error":{"status":"UNAVAILABLE"}}"""),
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val provider = FcmPushProvider(testServiceAccountJson, mockCredentials, client)
        val result = provider.sendPush("user-4", "token-4", "Title", "Body")

        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("unavailable", result.reason)
    }

    @Test
    fun `given token acquisition failure when sendPush then returns TemporaryFailure`() = runTest {
        val mockCredentials = mockk<GoogleCredentials>()
        every { mockCredentials.refreshIfExpired() } returns Unit
        every { mockCredentials.getAccessToken() } throws RuntimeException("Invalid service account JSON")

        val client = HttpClient(MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{}"""),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val provider = FcmPushProvider(testServiceAccountJson, mockCredentials, client)
        val result = provider.sendPush("user-5", "token-5", "Title", "Body")

        assertIs<PushResult.TemporaryFailure>(result)
        assertEquals("Invalid service account JSON", result.reason)
    }

    @Test
    fun `given network timeout when sendPush then returns TemporaryFailure`() = runTest {
        val mockCredentials = mockk<GoogleCredentials>()
        every { mockCredentials.refreshIfExpired() } returns Unit
        every { mockCredentials.getAccessToken() } returns AccessToken("test-token", null)

        val mockEngine = MockEngine { _ ->
            throw IOException("Connection refused")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        val provider = FcmPushProvider(testServiceAccountJson, mockCredentials, client)
        val result = provider.sendPush("user-6", "token-6", "Title", "Body")

        assertIs<PushResult.TemporaryFailure>(result)
    }
}
