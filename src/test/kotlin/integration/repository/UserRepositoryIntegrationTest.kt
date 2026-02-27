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
    fun `create persists user and can be found by email`() {
        val user = User(
            id = "user-1",
            email = "user1@test.com",
            passwordHash = "hash-1",
            displayName = "User One",
            createdAt = 1L
        )

        repository.create(user)
        val found = repository.findByEmail("user1@test.com")

        assertNotNull(found)
        assertEquals("user-1", found.id)
        assertEquals("User One", found.displayName)
    }

    @Test
    fun `findPartnerByUserId returns partner when partnerId exists`() {
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

        repository.create(partner)
        repository.create(user)

        val found = repository.findPartnerByUserId("user-1")

        assertNotNull(found)
        assertEquals("partner-1", found.id)
        assertEquals("partner@test.com", found.email)
    }

    @Test
    fun `findPartnerByUserId returns null when partnerId is not set`() {
        val user = User(
            id = "user-1",
            email = "user@test.com",
            passwordHash = "hash-u",
            displayName = "User",
            createdAt = 1L
        )

        repository.create(user)
        val found = repository.findPartnerByUserId("user-1")

        assertNull(found)
    }
}
