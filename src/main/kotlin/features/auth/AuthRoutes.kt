package com.pecadoartesano.features.auth

import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.features.auth.dto.LoginRequest
import com.pecadoartesano.features.auth.dto.RegisterRequest
import com.pecadoartesano.features.auth.dto.TokenResponse
import com.pecadoartesano.features.auth.ports.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.authRoutes( authService: AuthService) {

    route("/auth") {

        // User registration endpoint
        post("/register") {
            val request = call.receive<RegisterRequest>()
            println(request)

            try {
                val token = authService.register(
                    email = request.email,
                    rawPassword = request.password,
                    displayName = request.displayName
                )

                call.respond(HttpStatusCode.Created, TokenResponse(accessToken = token))

            } catch (e: IllegalArgumentException) {
                print("error 1 -->")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: EmailAlreadyExistsException) {
                print("error 3 -->")
                call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
            } catch (e: Exception) {
                print("error 2 -->")
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
