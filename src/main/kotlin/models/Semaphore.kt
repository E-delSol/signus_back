package com.pecadoartesano.models

import kotlinx.serialization.Serializable

@Serializable
data class Semaphore(
    val status: SemaphoreStatus,
    val userId: String,
    val expiration: Long? = null, // Timestamp (ms) cuando expira el estado (null si es indefinido)
    val duration: Long? = null // Duración seleccionada original (ms), para mostrar "1h" etc.
)
