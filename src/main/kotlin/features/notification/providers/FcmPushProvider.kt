package com.pecadoartesano.features.notification.providers

import com.pecadoartesano.features.notification.PushProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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

    override suspend fun sendPush(targetUserId: String, token: String, title: String, body: String): Boolean {
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

        return response.status.value in 200..299 && response.body<FcmResponse>().failure == 0
    }
}

@Serializable
private data class FcmRequest(
    val to: String,
    val notification: FcmNotification,
    val data: Map<String, String>
)

@Serializable
private data class FcmNotification(
    val title: String,
    val body: String
)

@Serializable
private data class FcmResponse(
    @SerialName("failure") val failure: Int
)
