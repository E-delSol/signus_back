package com.pecadoartesano.features.user.ports

import com.pecadoartesano.features.user.User

interface UserService {
    fun register(
        email: String,
        rawPassword: String,
        displayName: String? = null
    ): User
}
