package com.pecadoartesano.features.auth

import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.ports.AuthUserRepositoryPort
import com.pecadoartesano.features.auth.ports.RefreshTokenRepositoryPort
import com.pecadoartesano.features.user.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AuthServiceImplTest {

    private val userRepository = mockk<AuthUserRepositoryPort>()
    private val passwordService = mockk<PasswordService>()
    private val jwtService = mockk<JwtService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepositoryPort>()
    private var now = 1_000L
    private val authService = AuthServiceImpl(
        userRepository = userRepository,
        passwordService = passwordService,
        jwtService = jwtService,
        refreshTokenRepository = refreshTokenRepository,
        refreshTokenExpirationMillis = 5_000L,
        nowProvider = { now },
        refreshTokenGenerator = { "generated-refresh-token" }
    )

    @Test
    fun `given blank email and password when register then throws and does not create user`() {
        assertFailsWith<IllegalArgumentException> {
            authService.register(email = "", rawPassword = "", displayName = null)
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given blank email when register then throws and does not create user`() {
        assertFailsWith<IllegalArgumentException> {
            authService.register(email = "", rawPassword = "secret", displayName = null)
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given blank password when register then throws and does not create user`() {
        assertFailsWith<IllegalArgumentException> {
            authService.register(email = "user@test.com", rawPassword = "", displayName = null)
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given existing email when register then throws and does not create user`() {
        every { userRepository.findByEmail("existing@test.com") } returns testUser()

        assertFailsWith<EmailAlreadyExistsException> {
            authService.register(
                email = "existing@test.com",
                rawPassword = "secret",
                displayName = "Existing"
            )
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given valid data when register then creates user stores refresh session and returns both tokens`() {
        every { userRepository.findByEmail("new@test.com") } returns null
        every { passwordService.hash("secret") } returns "hashed-secret"
        every { userRepository.create(any()) } answers { firstArg() }
        every { refreshTokenRepository.create(any()) } answers { firstArg() }
        every { jwtService.generateAccessToken(any()) } returns "access-token-123"

        val result = authService.register(
            email = "new@test.com",
            rawPassword = "secret",
            displayName = "New User"
        )

        val userSlot = slot<User>()
        val refreshSessionSlot = slot<RefreshTokenSession>()
        verify(exactly = 1) { userRepository.create(capture(userSlot)) }
        verify(exactly = 1) { refreshTokenRepository.create(capture(refreshSessionSlot)) }
        assertNotNull(userSlot.captured.id)
        assertEquals("new@test.com", userSlot.captured.email)
        assertEquals("hashed-secret", userSlot.captured.passwordHash)
        assertEquals("New User", userSlot.captured.displayName)
        assertEquals(userSlot.captured.id, refreshSessionSlot.captured.userId)
        assertEquals(1_000L, refreshSessionSlot.captured.createdAt)
        assertEquals(6_000L, refreshSessionSlot.captured.expiresAt)
        assertEquals("access-token-123", result.accessToken)
        assertEquals("generated-refresh-token", result.refreshToken)
    }

    @Test
    fun `given missing user when login then throws`() {
        every { userRepository.findByEmail("missing@test.com") } returns null

        assertFailsWith<IllegalArgumentException> {
            authService.login(email = "missing@test.com", rawPassword = "secret")
        }
    }

    @Test
    fun `given invalid password when login then throws`() {
        val user = testUser(email = "user@test.com", passwordHash = "stored-hash")
        every { userRepository.findByEmail("user@test.com") } returns user
        every { passwordService.verify("secret", "stored-hash") } returns false

        assertFailsWith<IllegalArgumentException> {
            authService.login(email = "user@test.com", rawPassword = "secret")
        }
    }

    @Test
    fun `given valid credentials when login then returns access and refresh tokens`() {
        val user = testUser(email = "user@test.com", passwordHash = "stored-hash")
        every { userRepository.findByEmail("user@test.com") } returns user
        every { passwordService.verify("secret", "stored-hash") } returns true
        every { refreshTokenRepository.create(any()) } answers { firstArg() }
        every { jwtService.generateAccessToken(user.id) } returns "token-ok"

        val result = authService.login(email = "user@test.com", rawPassword = "secret")

        assertEquals("token-ok", result.accessToken)
        assertEquals("generated-refresh-token", result.refreshToken)
    }

    @Test
    fun `given valid refresh token when refresh then returns new access token`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns refreshSession()
        every { jwtService.generateAccessToken("user-id") } returns "new-access-token"

        val result = authService.refresh("valid-refresh-token")

        assertEquals("new-access-token", result)
    }

    @Test
    fun `given missing refresh token when refresh then throws invalid refresh token`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns null

        assertFailsWith<InvalidRefreshTokenException> {
            authService.refresh("unknown-refresh-token")
        }
    }

    @Test
    fun `given expired refresh token when refresh then throws expired exception`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns refreshSession(expiresAt = 999L)

        assertFailsWith<RefreshTokenExpiredException> {
            authService.refresh("expired-refresh-token")
        }
    }

    @Test
    fun `given revoked refresh token when refresh then throws revoked exception`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns refreshSession(revokedAt = 900L)

        assertFailsWith<RefreshTokenRevokedException> {
            authService.refresh("revoked-refresh-token")
        }
    }

    @Test
    fun `given valid user and refresh token when logout then revokes refresh token`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns refreshSession()
        every { refreshTokenRepository.revokeById("session-id", 1_000L) } returns true

        authService.logout("user-id", "valid-refresh-token")

        verify(exactly = 1) { refreshTokenRepository.revokeById("session-id", 1_000L) }
    }

    @Test
    fun `given refresh token from different user when logout then throws invalid refresh token`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns refreshSession(userId = "other-user")

        assertFailsWith<InvalidRefreshTokenException> {
            authService.logout("user-id", "valid-refresh-token")
        }
    }

    private fun refreshSession(
        id: String = "session-id",
        userId: String = "user-id",
        expiresAt: Long = 2_000L,
        revokedAt: Long? = null
    ) = RefreshTokenSession(
        id = id,
        userId = userId,
        tokenHash = "hashed-token",
        expiresAt = expiresAt,
        createdAt = 500L,
        revokedAt = revokedAt
    )

    private fun testUser(
        id: String = "user-id",
        email: String = "test@test.com",
        passwordHash: String = "hash"
    ) = User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        displayName = "Test",
        createdAt = 1L
    )
}
