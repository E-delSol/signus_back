package com.pecadoartesano.models

import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.Serializable

@Serializable
data class UserConnection(
    val userId: String,
    val socketSession: WebSocketSession
)
