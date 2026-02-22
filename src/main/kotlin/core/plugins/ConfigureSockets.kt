package com.pecadoartesano.core.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.features.notification.RealtimeNotificationService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import java.util.concurrent.TimeUnit

fun Application.configureSockets(jwtConfig: JwtConfig, realtimeNotificationService: RealtimeNotificationService) {
    install(WebSockets) {
        pingPeriodMillis = TimeUnit.SECONDS.toMillis(15)
        timeoutMillis = TimeUnit.SECONDS.toMillis(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val verifier = JWT
        .require(Algorithm.HMAC256(jwtConfig.secret))
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .build()

    routing {
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
                for (_ in incoming) {
                    // Keep connection alive; notifications are server-pushed.
                }
            } finally {
                realtimeNotificationService.removeSession(userId)
            }
        }
    }
}
