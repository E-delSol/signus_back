package integration.repository

import com.pecadoartesano.features.user.User
import com.pecadoartesano.features.user.UserRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import support.PostgresIntegrationSupport

class UserRepositoryIntegrationTest {

    private val repository = UserRepository()

    @BeforeTest
    fun setUp() {
        PostgresIntegrationSupport.ensureDockerAndDatabase()
        PostgresIntegrationSupport.cleanDatabase()
    }

    @Test
    fun `given new user when create then user can be found by email`() {
        // Given
        val user = User(
            id = "user-1",
            email = "user1@test.com",
            passwordHash = "hash-1",
            displayName = "User One",
            createdAt = 1L
        )

        // When
        repository.create(user)
        val found = repository.findByEmail("user1@test.com")

        // Then
        assertNotNull(found)
        assertEquals("user-1", found.id)
        assertEquals("User One", found.displayName)
    }

    @Test
    fun `given user with partner when findPartnerByUserId then returns partner`() {
        // Given
        val partner = User(
            id = "partner-1",
            email = "partner@test.com",
            passwordHash = "hash-p",
            displayName = "Partner",
            createdAt = 1L
        )
        val user = User(
            id = "user-1",
            email = "user@test.com",
            passwordHash = "hash-u",
            displayName = "User",
            partnerId = "partner-1",
            createdAt = 1L
        )

        // When
        repository.create(partner)
        repository.create(user)

        val found = repository.findPartnerByUserId("user-1")

        // Then
        assertNotNull(found)
        assertEquals("partner-1", found.id)
        assertEquals("partner@test.com", found.email)
    }

    @Test
    fun `given user without partner when findPartnerByUserId then returns null`() {
        // Given
        val user = User(
            id = "user-1",
            email = "user@test.com",
            passwordHash = "hash-u",
            displayName = "User",
            createdAt = 1L
        )

        // When
        repository.create(user)
        val found = repository.findPartnerByUserId("user-1")

        // Then
        assertNull(found)
    }

    @Test
    fun `given linked users when unlink users then clears partner on both sides`() {
        // Given
        val userA = User(
            id = "user-a",
            email = "usera@test.com",
            passwordHash = "hash-a",
            displayName = "User A",
            partnerId = "user-b",
            createdAt = 1L
        )
        val userB = User(
            id = "user-b",
            email = "userb@test.com",
            passwordHash = "hash-b",
            displayName = "User B",
            partnerId = "user-a",
            createdAt = 1L
        )

        repository.create(userA)
        repository.create(userB)

        // When
        val unlinkedPartnerId = repository.unlinkUsers("user-a")
        val userAAfter = repository.findById("user-a")
        val userBAfter = repository.findById("user-b")

        // Then
        assertEquals("user-b", unlinkedPartnerId)
        assertNotNull(userAAfter)
        assertNotNull(userBAfter)
        assertNull(userAAfter.partnerId)
        assertNull(userBAfter.partnerId)
    }
}
