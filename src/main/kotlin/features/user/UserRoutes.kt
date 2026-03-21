package com.pecadoartesano.features.user

import com.pecadoartesano.features.user.dto.toMeResponse
import com.pecadoartesano.features.user.ports.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get

fun Route.userRoutes(userService: UserService) {
    authenticate {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            try {
                val currentUser = userService.getCurrentUser(userId)
                call.respond(HttpStatusCode.OK, currentUser.toMeResponse())
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User was not found"))
            }
        }

        get("/partner") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            try {
                val partner = userService.getCurrentPartner(userId)
                call.respond(HttpStatusCode.OK, partner.toMeResponse())
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            }
        }

        delete("/partner") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }

            try {
                userService.unlinkCurrentUser(userId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            }
        }
    }
}
