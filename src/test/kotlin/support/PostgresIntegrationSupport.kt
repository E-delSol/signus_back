package support

import com.pecadoartesano.core.config.DatabaseConfig
import com.pecadoartesano.core.database.configureDatabase
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume.assumeTrue
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer

object PostgresIntegrationSupport {
    // Requires Docker. Testcontainers needs a Docker API client >= 1.44.
    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("signus_test")
            withUsername("test")
            withPassword("test")
            start()
        }
    }

    @Volatile
    private var initialized = false

    fun ensureDockerAndDatabase() {
        assumeTrue(
            "Docker is required for repository integration tests. " +
                "Ensure Docker is running and the API client is >= 1.44 (Testcontainers requirement).",
            DockerClientFactory.instance().isDockerAvailable
        )
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val db = container
                    configureDatabase(
                        DatabaseConfig(
                            host = db.host,
                            port = db.firstMappedPort,
                            name = db.databaseName,
                            user = db.username,
                            password = db.password
                        )
                    )
                    initialized = true
                }
            }
        }
    }

    fun cleanDatabase() {
        transaction {
            exec("TRUNCATE TABLE semaphores CASCADE")
            exec("TRUNCATE TABLE users CASCADE")
        }
    }
}
