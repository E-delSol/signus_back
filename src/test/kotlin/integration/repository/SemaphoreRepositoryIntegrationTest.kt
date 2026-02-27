package integration.repository

import com.pecadoartesano.features.semaphore.SemaphoreRepository
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.semaphore.SemaphoreTable
import com.pecadoartesano.features.user.User
import com.pecadoartesano.features.user.UserRepository
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import support.PostgresIntegrationSupport

class SemaphoreRepositoryIntegrationTest {

    private val semaphoreRepository = SemaphoreRepository()
    private val userRepository = UserRepository()

    @BeforeTest
    fun setUp() {
        PostgresIntegrationSupport.ensureDockerAndDatabase()
        PostgresIntegrationSupport.cleanDatabase()
        userRepository.create(
            User(
                id = "user-1",
                email = "user1@test.com",
                passwordHash = "hash-1",
                displayName = "User One",
                createdAt = 1L
            )
        )
    }

    @Test
    fun `given user without semaphore when updateUserStatus then inserts semaphore`() {
        // Given

        // When
        val result = semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY)

        // Then
        assertEquals(SemaphoreStatus.BUSY, result.status)
        assertEquals("user-1", result.userId)

        val rows = transaction {
            SemaphoreTable
                .selectAll()
                .where { SemaphoreTable.userId eq "user-1" }
                .toList()
        }
        assertEquals(1, rows.size)
        assertEquals("BUSY", rows.single()[SemaphoreTable.status])
    }

    @Test
    fun `given existing semaphore when updateUserStatus then updates without duplicating rows`() {
        // Given
        semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.BUSY)

        // When
        val result = semaphoreRepository.updateUserStatus("user-1", SemaphoreStatus.AVAILABLE)

        // Then
        assertEquals(SemaphoreStatus.AVAILABLE, result.status)

        val rows = transaction {
            SemaphoreTable
                .selectAll()
                .where { SemaphoreTable.userId eq "user-1" }
                .toList()
        }
        assertEquals(1, rows.size)
        assertEquals("AVAILABLE", rows.single()[SemaphoreTable.status])
    }
}
