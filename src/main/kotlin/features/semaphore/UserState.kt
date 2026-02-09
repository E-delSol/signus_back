package com.pecadoartesano.features.semaphore

import kotlinx.serialization.Serializable

@Serializable
data class UserState(
    val id: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val partnerId: String? = null, // ID del usuario vinculado
    val status: SemaphoreStatus? = null, // Estado actual del semáforo
    val statusExpiration: Long? = null, // Timestamp (ms) cuando expira el estado (null si es indefinido)
    val statusDuration: Long? = null // Duración seleccionada original (ms), para mostrar "1h" etc.
)
