package com.pecadoartesano.features.auth

import com.pecadoartesano.features.auth.dto.LoginRequest
import com.pecadoartesano.features.auth.dto.RegisterRequest
import com.pecadoartesano.features.auth.dto.TokenResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.authRoutes( authService: AuthService) {

    route("/auth") {

        // User registration endpoint
        post("/register") {
            val request = call.receive<RegisterRequest>()

            try {
                val token = authService.register(
                    email = request.email,
                    rawPassword = request.password,
                    displayName = request.displayName
                )

                call.respond(HttpStatusCode.OK, TokenResponse(accessToken = token))

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "An unexpected error occurred"))
            }

        }

        // User login endpoint
        post("/login") {
            val request = call.receive<LoginRequest>()

            try {
                val token = authService.login(
                    email = request.email,
                    rawPassword = request.password
                )
                call.respond(HttpStatusCode.OK, TokenResponse(accessToken = token))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "An unexpected error occurred"))
            }
        }
    }

}
