package com.pecadoartesano.features.notification.providers

import com.pecadoartesano.features.notification.FcmErrorClassifier
import com.pecadoartesano.features.notification.PushResult
import com.pecadoartesano.features.notification.PushProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class FcmPushProvider(
    private val serverKey: String,
    private val endpoint: String = "https://fcm.googleapis.com/fcm/send",
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
) : PushProvider {

    override suspend fun sendPush(targetUserId: String, token: String, title: String, body: String): PushResult {
        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Authorization, "key=$serverKey")
            setBody(
                FcmRequest(
                    to = token,
                    notification = FcmNotification(title, body),
                    data = mapOf("targetUserId" to targetUserId)
                )
            )
        }

        return parseFcmHttpResponse(response, token)
    }
}

/**
 * Check if an HTTP status code indicates success, returning a [PushResult.TemporaryFailure] if not.
 */
internal fun checkHttpStatus(token: String, statusCode: Int): PushResult? {
    if (statusCode !in 200..299) {
        return PushResult.TemporaryFailure(
            token = token,
            reason = "http_error_$statusCode",
            errorCode = "HTTP_ERROR"
        )
    }
    return null
}

/**
 * Parse an [FcmResponse] body into a [PushResult] for the given [token].
 *
 * Pure function — no HTTP dependency. Extracted for testability.
 */
internal fun parseFcmResponse(token: String, fcmResponse: FcmResponse): PushResult {
    val result = fcmResponse.results.firstOrNull()
        ?: return PushResult.PermanentFailure(
            token = token,
            reason = "empty_results",
            errorCode = "EMPTY_RESULTS"
        )
    return result.error?.let { error ->
        FcmErrorClassifier.classify(token, error)
    } ?: PushResult.Success(token)
}

/**
 * Parse an FCM HTTP response into a [PushResult].
 *
 * Combines HTTP status check with response body parsing.
 */
internal suspend fun parseFcmHttpResponse(response: HttpResponse, token: String): PushResult {
    checkHttpStatus(token, response.status.value)?.let { return it }
    return parseFcmResponse(token, response.body())
}

@Serializable
internal data class FcmRequest(
    val to: String,
    val notification: FcmNotification,
    val data: Map<String, String>
)

@Serializable
internal data class FcmNotification(
    val title: String,
    val body: String
)

@Serializable
internal data class FcmResponse(
    @SerialName("success") val success: Int = 0,
    @SerialName("failure") val failure: Int = 0,
    @SerialName("results") val results: List<FcmResult> = emptyList()
)

@Serializable
internal data class FcmResult(
    @SerialName("error") val error: String? = null
)
