package com.pecadoartesano.core.plugins

import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.features.auth.authRoutes
import com.pecadoartesano.features.auth.ports.AuthService
import com.pecadoartesano.features.linking.linkingRoutes
import com.pecadoartesano.features.linking.ports.LinkingService
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.notification.notificationSocketRoutes
import com.pecadoartesano.features.semaphore.ports.StatusService
import com.pecadoartesano.features.semaphore.statusRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    statusService: StatusService,
    jwtConfig: JwtConfig,
    realtimeNotificationService: RealtimeNotificationService,
    linkingService: LinkingService
) {
    routing {
        authRoutes(authService)
        statusRoutes(statusService)
        linkingRoutes(linkingService)
        notificationSocketRoutes(jwtConfig, realtimeNotificationService)
    }
}
