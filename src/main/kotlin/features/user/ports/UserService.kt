package com.pecadoartesano.features.user.ports

import com.pecadoartesano.features.semaphore.UserState
import com.pecadoartesano.features.user.User

interface UserService {
    fun register(
        email: String,
        rawPassword: String,
        displayName: String? = null
    ): User

    fun getCurrentUser(userId: String): UserState

    fun getCurrentPartner(userId: String): UserState
}
