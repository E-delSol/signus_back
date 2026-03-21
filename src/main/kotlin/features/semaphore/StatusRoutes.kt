package com.pecadoartesano.features.semaphore

import com.pecadoartesano.features.semaphore.dto.StatusPatchRequest
import com.pecadoartesano.features.semaphore.ports.StatusService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch

fun Route.statusRoutes(statusService: StatusService) {
    authenticate {
        patch("/status") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized)
                return@patch
            }

            val request = call.receive<StatusPatchRequest>()
            val updated = statusService.updateStatus(userId, request.status)
            call.respond(HttpStatusCode.OK, updated)
        }
    }
}
