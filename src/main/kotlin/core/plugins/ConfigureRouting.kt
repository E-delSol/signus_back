package com.pecadoartesano.core.plugins

import com.pecadoartesano.features.auth.authRoutes
import com.pecadoartesano.features.auth.dto.AuthService
import com.pecadoartesano.features.semaphore.StatusService
import com.pecadoartesano.features.semaphore.statusRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    statusService: StatusService
) {
    routing {
        authRoutes(authService)
        statusRoutes(statusService)
    }
}
