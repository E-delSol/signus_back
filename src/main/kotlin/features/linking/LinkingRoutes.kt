package com.pecadoartesano.features.linking

import com.pecadoartesano.features.linking.dto.ConfirmLinkSessionRequest
import com.pecadoartesano.features.linking.dto.toCreateResponse
import com.pecadoartesano.features.linking.dto.toStatusResponse
import com.pecadoartesano.features.linking.ports.LinkingService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.linkingRoutes(linkingService: LinkingService) {
    route("/linking") {
        authenticate {
            post("/sessions") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val ownerUserId = userId.toUuidOrNull()
                if (ownerUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid userId in token"))
                    return@post
                }

                val session = linkingService.createSession(ownerUserId)
                call.respond(HttpStatusCode.Created, toCreateResponse(session.id, session.linkCode, session.expiresAt))
            }

            post("/sessions/confirm") {
                val request = call.receive<ConfirmLinkSessionRequest>()
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val confirmedByUserId = userId.toUuidOrNull()
                if (confirmedByUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid userId in token"))
                    return@post
                }

                try {
                    val session = linkingService.confirmSessionByLinkCode(request.linkCode, confirmedByUserId)
                    call.respond(HttpStatusCode.OK, toStatusResponse(session.id, session.status))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: LinkSessionNotFoundException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                } catch (e: LinkSessionExpiredException) {
                    call.respond(HttpStatusCode.Gone, mapOf("error" to e.message))
                } catch (e: LinkSessionAlreadyConfirmedException) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
                }
            }

            get("/sessions/{id}") {
                val rawId = call.parameters["id"]
                val sessionId = rawId?.toUuidOrNull()

                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid sessionId"))
                    return@get
                }

                try {
                    val session = linkingService.getSession(sessionId)
                    call.respond(HttpStatusCode.OK, toStatusResponse(session.id, session.status))
                } catch (e: LinkSessionNotFoundException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                }
            }
        }
    }
}

private fun String.toUuidOrNull(): UUID? = try {
    UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    null
}
