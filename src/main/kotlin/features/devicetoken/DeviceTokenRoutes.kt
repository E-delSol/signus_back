package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.features.devicetoken.dto.toResponse
import com.pecadoartesano.features.devicetoken.dto.UpsertDeviceTokenRequest
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.deviceTokenRoutes(deviceTokenService: DeviceTokenService) {
    authenticate {
        route("/devices/fcm-token") {
            put {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }

                try {
                    val request = call.receive<UpsertDeviceTokenRequest>()
                    val result = deviceTokenService.upsertForUser(
                        userId = userId,
                        deviceId = request.deviceId,
                        fcmToken = request.fcmToken,
                        platform = request.platform,
                        appVersion = request.appVersion
                    )

                    val status = if (result.created) HttpStatusCode.Created else HttpStatusCode.OK
                    call.respond(status, result.toResponse())
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            delete("/{deviceId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }

                val deviceId = call.parameters["deviceId"]
                if (deviceId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "deviceId path parameter is required"))
                    return@delete
                }

                try {
                    deviceTokenService.deactivateForUserDevice(userId, deviceId)
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val includeInactiveRaw = call.request.queryParameters["includeInactive"]
                val includeInactive = when {
                    includeInactiveRaw == null -> false
                    includeInactiveRaw.equals("true", ignoreCase = true) -> true
                    includeInactiveRaw.equals("false", ignoreCase = true) -> false
                    else -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "includeInactive must be true or false")
                        )
                        return@get
                    }
                }

                try {
                    val tokens = deviceTokenService.listForUser(userId, includeInactive)
                    call.respond(HttpStatusCode.OK, tokens.map { it.toResponse() })
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
        }
    }
}
