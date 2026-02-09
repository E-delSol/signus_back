package com.pecadoartesano.features.auth

import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.user.User
import com.pecadoartesano.features.user.UserRepository
import java.util.UUID

class AuthService(
    private val userRepository: UserRepository,
    private val passwordService: PasswordService,
    private val jwtService: JwtService
) {
    fun register(
        email: String,
        rawPassword: String,
        displayName: String? = null
    ): String {

        if (email.isBlank() || rawPassword.isBlank()) {
            error("Email and password must not be blank.")
        }

        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("Email already exists.")
        }

        val user = User(
            id = generateUserId(),
            email = email,
            passwordHash = passwordService.hash(rawPassword),
            displayName = displayName,
            createdAt = System.currentTimeMillis()
        )

        userRepository.create(user)

        return jwtService.generateToken(user)
    }

    fun login(email: String, rawPassword: String): String {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password.")

        if (!passwordService.verify(rawPassword, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password.")
        }

        return jwtService.generateToken(user)
    }

    private fun generateUserId(): String =
        UUID.randomUUID().toString()
}