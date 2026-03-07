package com.pecadoartesano.features.user

import at.favre.lib.crypto.bcrypt.BCrypt
import com.pecadoartesano.features.user.ports.UserService
import java.util.UUID

class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    override fun register(
        email: String,
        rawPassword: String,
        displayName: String?
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
