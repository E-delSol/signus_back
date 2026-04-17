package com.pecadoartesano.features.auth

import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.ports.AuthService
import com.pecadoartesano.features.auth.ports.AuthUserRepositoryPort
import com.pecadoartesano.features.auth.ports.RefreshTokenRepositoryPort
import com.pecadoartesano.features.user.User
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class AuthServiceImpl(
    private val userRepository: AuthUserRepositoryPort,
    private val passwordService: PasswordService,
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val refreshTokenExpirationMillis: Long,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val refreshTokenGenerator: () -> String = { generateRefreshTokenValue() }
) : AuthService {
    override fun register(
        email: String,
        rawPassword: String,
        displayName: String?
    ): AuthSessionTokens {

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
            createdAt = nowProvider()
        )

        userRepository.create(user)

        return createSessionTokens(user.id)
    }

    override fun login(email: String, rawPassword: String): AuthSessionTokens {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password.")

        if (!passwordService.verify(rawPassword, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password.")
        }

        return createSessionTokens(user.id)
    }

    override fun refresh(refreshToken: String): String {
        val normalizedToken = refreshToken.trim()
        require(normalizedToken.isNotBlank()) { "refreshToken must not be blank" }

        val session = refreshTokenRepository.findByTokenHash(hashRefreshToken(normalizedToken))
            ?: throw InvalidRefreshTokenException("Refresh token is invalid.")

        if (session.revokedAt != null) {
            throw RefreshTokenRevokedException("Refresh token has been revoked.")
        }

        if (session.expiresAt <= nowProvider()) {
            throw RefreshTokenExpiredException("Refresh token has expired.")
        }

        return jwtService.generateAccessToken(session.userId)
    }

    override fun logout(userId: String, refreshToken: String) {
        require(userId.isNotBlank()) { "userId must not be blank" }

        val normalizedToken = refreshToken.trim()
        require(normalizedToken.isNotBlank()) { "refreshToken must not be blank" }

        val session = refreshTokenRepository.findByTokenHash(hashRefreshToken(normalizedToken))
            ?: throw InvalidRefreshTokenException("Refresh token is invalid.")

        if (session.userId != userId) {
            throw InvalidRefreshTokenException("Refresh token is invalid.")
        }

        if (session.revokedAt != null) {
            throw RefreshTokenRevokedException("Refresh token has already been revoked.")
        }

        refreshTokenRepository.revokeById(session.id, nowProvider())
    }

    private fun createSessionTokens(userId: String): AuthSessionTokens {
        val refreshToken = refreshTokenGenerator()
        val now = nowProvider()

        refreshTokenRepository.create(
            RefreshTokenSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                tokenHash = hashRefreshToken(refreshToken),
                expiresAt = now + refreshTokenExpirationMillis,
                createdAt = now
            )
        )

        return AuthSessionTokens(
            accessToken = jwtService.generateAccessToken(userId),
            refreshToken = refreshToken
        )
    }

    private fun generateUserId(): String =
        UUID.randomUUID().toString()

    private fun hashRefreshToken(refreshToken: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(refreshToken.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        private const val REFRESH_TOKEN_NUM_BYTES = 32
        private val secureRandom = SecureRandom()

        private fun generateRefreshTokenValue(): String {
            val bytes = ByteArray(REFRESH_TOKEN_NUM_BYTES)
            secureRandom.nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
