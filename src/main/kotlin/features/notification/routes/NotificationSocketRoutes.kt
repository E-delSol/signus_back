package com.pecadoartesano.features.notification.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.features.notification.RealtimeNotificationService
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close

fun Route.notificationSocketRoutes(
    jwtConfig: JwtConfig,
    realtimeNotificationService: RealtimeNotificationService
) {
    val verifier = JWT
        .require(Algorithm.HMAC256(jwtConfig.secret))
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .build()

    webSocket("/ws") {
        val token = call.request.queryParameters["token"]
        if (token.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing JWT token"))
            return@webSocket
        }

        val userId = runCatching {
            verifier.verify(token).getClaim("userId").asString()
        }.getOrNull()

        if (userId.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid JWT token"))
            return@webSocket
        }

        realtimeNotificationService.registerSession(userId, this)
        try {
            for (frame in incoming) {
                // Keep connection alive; notifications are server-pushed.
            }
        } finally {
            realtimeNotificationService.removeSession(userId)
        }
    }
}
