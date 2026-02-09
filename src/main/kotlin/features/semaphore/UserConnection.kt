package com.pecadoartesano.features.semaphore

import io.ktor.websocket.*

data class UserConnection(
    val userId: String,
    val socketSession: WebSocketSession
)
