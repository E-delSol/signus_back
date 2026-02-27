package com.pecadoartesano.core.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import java.util.concurrent.TimeUnit

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriodMillis = TimeUnit.SECONDS.toMillis(15)
        timeoutMillis = TimeUnit.SECONDS.toMillis(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
