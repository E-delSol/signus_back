package com.pecadoartesano.features.auth

import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.features.auth.dto.LoginRequest
import com.pecadoartesano.features.auth.dto.LogoutRequest
import com.pecadoartesano.features.auth.dto.RefreshSessionRequest
import com.pecadoartesano.features.auth.dto.RefreshSessionResponse
import com.pecadoartesano.features.auth.dto.RegisterRequest
import com.pecadoartesano.features.auth.dto.toResponse
import com.pecadoartesano.features.auth.ports.AuthService
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.authRoutes(authService: AuthService) {

    route("/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()

            try {
                val session = authService.register(
                    email = request.email,
                    rawPassword = request.password,
                    displayName = request.displayName
                )

                call.respond(HttpStatusCode.Created, session.toResponse())

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: EmailAlreadyExistsException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "An unexpected error occurred"))
            }

        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            try {
                val session = authService.login(
                    email = request.email,
                    rawPassword = request.password
                )
                call.respond(HttpStatusCode.OK, session.toResponse())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "An unexpected error occurred"))
            }
        }

        post("/refresh") {
            val request = call.receive<RefreshSessionRequest>()

            try {
                val accessToken = authService.refresh(request.refreshToken)
                call.respond(HttpStatusCode.OK, RefreshSessionResponse(accessToken = accessToken))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: InvalidRefreshTokenException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: RefreshTokenExpiredException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: RefreshTokenRevokedException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "An unexpected error occurred"))
            }
        }

        authenticate {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receive<LogoutRequest>()

                try {
                    authService.logout(userId, request.refreshToken)
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: InvalidRefreshTokenException) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                } catch (e: RefreshTokenRevokedException) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "An unexpected error occurred"))
                }
            }
        }
    }

}
