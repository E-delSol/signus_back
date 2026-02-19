package com.pecadoartesano.core.plugins

import com.pecadoartesano.features.auth.authRoutes
import com.pecadoartesano.features.auth.dto.AuthService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(authService: AuthService) {
    routing {
        authRoutes(authService)
    }
}
