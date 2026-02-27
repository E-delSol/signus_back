package com.pecadoartesano.features.auth

import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.ports.AuthUserRepositoryPort
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
    private val authService = AuthServiceImpl(userRepository, passwordService, jwtService)

    @Test
    fun `given blank email and password when register then throws and does not create user`() {
        // Given

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            authService.register(email = "", rawPassword = "", displayName = null)
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given blank email when register then throws and does not create user`() {
        // Given

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            authService.register(email = "", rawPassword = "secret", displayName = null)
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given blank password when register then throws and does not create user`() {
        // Given

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            authService.register(email = "user@test.com", rawPassword = "", displayName = null)
        }

        verify(exactly = 0) { userRepository.create(any()) }
    }

    @Test
    fun `given existing email when register then throws and does not create user`() {
        // Given
        every { userRepository.findByEmail("existing@test.com") } returns testUser()

        // When / Then
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
    fun `given valid data when register then creates user and returns token`() {
        // Given
        every { userRepository.findByEmail("new@test.com") } returns null
        every { passwordService.hash("secret") } returns "hashed-secret"
        every { userRepository.create(any()) } answers { firstArg() }
        every { jwtService.generateToken(any()) } returns "token-123"

        // When
        val result = authService.register(
            email = "new@test.com",
            rawPassword = "secret",
            displayName = "New User"
        )

        // Then
        val userSlot = slot<User>()
        verify(exactly = 1) { userRepository.create(capture(userSlot)) }
        assertNotNull(userSlot.captured.id)
        assertEquals("new@test.com", userSlot.captured.email)
        assertEquals("hashed-secret", userSlot.captured.passwordHash)
        assertEquals("New User", userSlot.captured.displayName)
        assertEquals("token-123", result)
    }

    @Test
    fun `given missing user when login then throws`() {
        // Given
        every { userRepository.findByEmail("missing@test.com") } returns null

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            authService.login(email = "missing@test.com", rawPassword = "secret")
        }
    }

    @Test
    fun `given invalid password when login then throws`() {
        // Given
        val user = testUser(email = "user@test.com", passwordHash = "stored-hash")
        every { userRepository.findByEmail("user@test.com") } returns user
        every { passwordService.verify("secret", "stored-hash") } returns false

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            authService.login(email = "user@test.com", rawPassword = "secret")
        }
    }

    @Test
    fun `given valid credentials when login then returns token`() {
        // Given
        val user = testUser(email = "user@test.com", passwordHash = "stored-hash")
        every { userRepository.findByEmail("user@test.com") } returns user
        every { passwordService.verify("secret", "stored-hash") } returns true
        every { jwtService.generateToken(user) } returns "token-ok"

        // When
        val result = authService.login(email = "user@test.com", rawPassword = "secret")

        // Then
        assertEquals("token-ok", result)
    }

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
