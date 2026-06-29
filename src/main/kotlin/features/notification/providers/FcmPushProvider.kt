package com.pecadoartesano.features.notification.providers

import com.google.auth.oauth2.GoogleCredentials
import com.pecadoartesano.features.notification.FcmErrorClassifier
import com.pecadoartesano.features.notification.PushResult
import com.pecadoartesano.features.notification.PushProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

class FcmPushProvider(
    private val projectId: String,
    private val credentials: GoogleCredentials,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
) : PushProvider {

    private val fcmEndpoint = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

    override suspend fun sendPush(targetUserId: String, token: String, title: String, body: String): PushResult {
        return try {
            val accessToken = credentials.getAccessToken().tokenValue
            val response = client.post(fcmEndpoint) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(buildV1Message(token, targetUserId, title, body))
            }
            val responseBody = try {
                response.body<FcmV1Response>()
            } catch (e: Exception) {
                null
            }
            parseFcmV1ResponseBody(token, response.status.value, responseBody)
        } catch (e: Exception) {
            PushResult.TemporaryFailure(token, e.message ?: "unknown", "EXCEPTION")
        }
    }
}

// ── FCM v1 models ──────────────────────────────────────────────────────────

@Serializable
internal data class FcmV1Request(
    val message: FcmV1Message
)

@Serializable
internal data class FcmV1Message(
    val token: String,
    val notification: FcmV1Notification? = null,
    val data: Map<String, String>? = null
)

@Serializable
internal data class FcmV1Notification(
    val title: String,
    val body: String
)

/**
 * Combined response model for FCM v1 HTTP API.
 *
 * Successful response: `{"name":"projects/.../messages/..."}`
 * Error response: `{"error":{"status":"UNREGISTERED",...}}`
 */
@Serializable
internal data class FcmV1Response(
    val name: String? = null,
    val error: FcmV1Error? = null
)

@Serializable
internal data class FcmV1Error(
    val status: String? = null,
    val message: String? = null
)

/**
 * Pure function to parse an FCM v1 HTTP response body into a [PushResult].
 *
 * @param token   The FCM token that was targeted.
 * @param statusCode  The HTTP status code from the FCM v1 response.
 * @param body    The deserialized response body (or null if unparseable).
 */
internal fun parseFcmV1ResponseBody(
    token: String,
    statusCode: Int,
    body: FcmV1Response?
): PushResult {
    if (body == null) {
        return if (statusCode in 200..299) {
            PushResult.PermanentFailure(token, "parse_error", "PARSE_ERROR")
        } else {
            PushResult.TemporaryFailure(token, "http_error_$statusCode", "HTTP_ERROR")
        }
    }

    if (statusCode in 200..299) {
        return if (body.name != null) {
            PushResult.Success(token)
        } else {
            PushResult.PermanentFailure(token, "parse_error", "PARSE_ERROR")
        }
    }

    // Error response
    val errorStatus = body.error?.status
    return if (errorStatus != null) {
        FcmErrorClassifier.classify(token, errorStatus)
    } else {
        PushResult.TemporaryFailure(token, "http_error_$statusCode", "HTTP_ERROR")
    }
}

/**
 * Build a [FcmV1Request] from individual push parameters.
 */
internal fun buildV1Message(
    token: String,
    targetUserId: String,
    title: String,
    body: String
): FcmV1Request {
    return FcmV1Request(
        message = FcmV1Message(
        token = token,
        notification = FcmV1Notification(title, body),
        data = mapOf("targetUserId" to targetUserId)
        )
    )
}
