package features.user

import com.pecadoartesano.configureApp
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.semaphore.UserState
import com.pecadoartesano.features.user.dto.MeResponse
import com.pecadoartesano.features.user.ports.UserService
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.delete
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.koin.dsl.module as koinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import support.createJwtToken
import support.decodeJson
import support.testAppConfig

class UserRoutesIntegrationTest {

    @Test
    fun `given valid token when get me then returns current user info`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeUserService = object : UserService {
            override fun register(email: String, rawPassword: String, displayName: String?) =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentUser(userId: String): UserState =
                UserState(
                    id = userId,
                    partnerId = "partner-1",
                    status = SemaphoreStatus.BUSY,
                    statusExpiration = 1_700_000_000_000,
                    statusDuration = 3_600_000
                )

            override fun getCurrentPartner(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override suspend fun unlinkCurrentUser(userId: String) =
                throw UnsupportedOperationException("Not used in this test")
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<UserService> { fakeUserService }
                    }
                )
            )
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.get("/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val body = decodeJson<MeResponse>(response.bodyAsText())
        assertEquals("user-1", body.id)
        assertEquals("partner-1", body.partnerId)
        assertEquals(SemaphoreStatus.BUSY, body.status)
        assertEquals(1_700_000_000_000, body.statusExpiration)
        assertEquals(3_600_000, body.statusDuration)
    }

    @Test
    fun `given missing token when get me then returns unauthorized`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val response = client.get("/me")

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given valid token when get partner then returns partner info`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeUserService = object : UserService {
            override fun register(email: String, rawPassword: String, displayName: String?) =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentUser(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentPartner(userId: String): UserState =
                UserState(
                    id = "partner-1",
                    partnerId = userId,
                    status = SemaphoreStatus.AVAILABLE,
                    statusExpiration = null,
                    statusDuration = null
                )

            override suspend fun unlinkCurrentUser(userId: String) =
                throw UnsupportedOperationException("Not used in this test")
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<UserService> { fakeUserService }
                    }
                )
            )
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.get("/partner") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val body = decodeJson<MeResponse>(response.bodyAsText())
        assertEquals("partner-1", body.id)
        assertEquals("user-1", body.partnerId)
        assertEquals(SemaphoreStatus.AVAILABLE, body.status)
        assertEquals(null, body.statusExpiration)
        assertEquals(null, body.statusDuration)
    }

    @Test
    fun `given no linked partner when get partner then returns not found`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeUserService = object : UserService {
            override fun register(email: String, rawPassword: String, displayName: String?) =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentUser(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentPartner(userId: String): UserState =
                throw IllegalStateException("User has no linked partner")

            override suspend fun unlinkCurrentUser(userId: String) =
                throw UnsupportedOperationException("Not used in this test")
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<UserService> { fakeUserService }
                    }
                )
            )
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.get("/partner") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Then
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `given valid token when delete partner then returns no content`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        var capturedUserId: String? = null
        val fakeUserService = object : UserService {
            override fun register(email: String, rawPassword: String, displayName: String?) =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentUser(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentPartner(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override suspend fun unlinkCurrentUser(userId: String) {
                capturedUserId = userId
            }
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<UserService> { fakeUserService }
                    }
                )
            )
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.delete("/partner") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Then
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("user-1", capturedUserId)
    }

    @Test
    fun `given user without linked partner when delete partner then returns not found`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeUserService = object : UserService {
            override fun register(email: String, rawPassword: String, displayName: String?) =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentUser(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentPartner(userId: String): UserState =
                throw UnsupportedOperationException("Not used in this test")

            override suspend fun unlinkCurrentUser(userId: String) {
                throw IllegalStateException("User has no linked partner")
            }
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<UserService> { fakeUserService }
                    }
                )
            )
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.delete("/partner") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Then
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
