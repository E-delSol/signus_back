package com.pecadoartesano.features.semaphore

enum class SemaphoreStatus{
    AVAILABLE,  // Verde
    BUSY;       // Rojo

    fun next(): SemaphoreStatus {
        return when (this) {
            AVAILABLE -> BUSY
            BUSY -> AVAILABLE
        }
    }
}
