package com.pecadoartesano.features.auth.ports

import com.pecadoartesano.features.user.User

interface AuthUserRepositoryPort {
    fun findByEmail(email: String): User?
    fun create(user: User): User
}
