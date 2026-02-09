package com.pecadoartesano.features.user

import at.favre.lib.crypto.bcrypt.BCrypt
import java.util.UUID

class UserService(
    private val userRepository: UserRepository
) {

    fun register(
        email: String,
        rawPassword: String,
        displayName: String? = null
    ): User {

        userRepository.findByEmail(email)?.let {
            error("User with email $email already exists.")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())

        val user = User(
            id = generateUserId(),
            email = email,
            passwordHash = passwordHash,
            displayName = displayName,
            createdAt = System.currentTimeMillis()
        )

        return userRepository.create(user)
    }

    private fun generateUserId(): String =
        UUID.randomUUID().toString()

}