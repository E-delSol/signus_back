package com.pecadoartesano.features.auth

import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.ports.AuthService
import com.pecadoartesano.features.auth.ports.AuthUserRepositoryPort
import com.pecadoartesano.features.user.User
import java.util.UUID

class AuthServiceImpl(
    private val userRepository: AuthUserRepositoryPort,
    private val passwordService: PasswordService,
    private val jwtService: JwtService
) : AuthService {
    override fun register(
        email: String,
        rawPassword: String,
        displayName: String?
    ): String {

        if (email.isBlank() || rawPassword.isBlank()) {
            print("Campo en blanco --> Email: '$email', Password: '$rawPassword'")
            throw IllegalArgumentException("Email and password must not be blank.")
        }

        if (userRepository.findByEmail(email) != null) {
            print("Email existente --> User: '$email' already exists.")
            throw EmailAlreadyExistsException("Email already exists.")
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

    override fun login(email: String, rawPassword: String): String {
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
