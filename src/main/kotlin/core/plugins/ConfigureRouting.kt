package com.pecadoartesano.core.plugins

import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.features.auth.authRoutes
import com.pecadoartesano.features.auth.dto.AuthService
import com.pecadoartesano.features.notification.dto.RealtimeNotificationService
import com.pecadoartesano.features.notification.notificationSocketRoutes
import com.pecadoartesano.features.semaphore.StatusService
import com.pecadoartesano.features.semaphore.statusRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    statusService: StatusService,
    jwtConfig: JwtConfig,
    realtimeNotificationService: RealtimeNotificationService
) {
    routing {
        authRoutes(authService)
        statusRoutes(statusService)
        notificationSocketRoutes(jwtConfig, realtimeNotificationService)
    }
}
