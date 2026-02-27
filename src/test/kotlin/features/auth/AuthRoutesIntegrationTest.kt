package features.auth

import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.configureApp
import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.features.auth.dto.AuthService
import com.pecadoartesano.features.auth.dto.TokenResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.koin.dsl.module as koinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import support.decodeJson
import support.testAppConfig

class AuthRoutesIntegrationTest {

    @Test
    fun `given valid register request when post then returns created with token`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String = "token-register"
            override fun login(email: String, rawPassword: String): String = "token-login"
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<AuthService> { fakeAuthService }
                    }
                )
            )
        }

        // When
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret","displayName":"User"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Created, response.status)
        val body = decodeJson<TokenResponse>(response.bodyAsText())
        assertEquals("token-register", body.accessToken)
    }

    @Test
    fun `given valid login request when post then returns ok with token`() = testApplication {
        // Given
        val appConfig: AppConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String = "token-register"
            override fun login(email: String, rawPassword: String): String = "token-login"
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<AuthService> { fakeAuthService }
                    }
                )
            )
        }

        // When
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret"}""")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val body = decodeJson<TokenResponse>(response.bodyAsText())
        assertEquals("token-login", body.accessToken)
    }

    @Test
    fun `given auth service throws illegal argument when register then returns bad request`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String {
                throw IllegalArgumentException("Invalid data")
            }

            override fun login(email: String, rawPassword: String): String = "token-login"
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<AuthService> { fakeAuthService }
                    }
                )
            )
        }

        // When
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"","password":"","displayName":null}""")
        }

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertTrue(body["error"].orEmpty().isNotBlank())
    }

    @Test
    fun `given auth service throws email already exists when register then returns conflict`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String {
                throw EmailAlreadyExistsException("Email already exists")
            }

            override fun login(email: String, rawPassword: String): String = "token-login"
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<AuthService> { fakeAuthService }
                    }
                )
            )
        }

        // When
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"existing@test.com","password":"secret","displayName":"User"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertEquals("Email already exists", body["error"])
    }

    @Test
    fun `given auth service throws generic exception when register then returns conflict`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String {
                throw RuntimeException("Unexpected")
            }

            override fun login(email: String, rawPassword: String): String = "token-login"
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<AuthService> { fakeAuthService }
                    }
                )
            )
        }

        // When
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret","displayName":"User"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertTrue(body["error"].orEmpty().isNotBlank())
    }

    @Test
    fun `given auth service throws illegal argument when login then returns unauthorized`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String = "token-register"

            override fun login(email: String, rawPassword: String): String {
                throw IllegalArgumentException("Invalid credentials")
            }
        }

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<AuthService> { fakeAuthService }
                    }
                )
            )
        }

        // When
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"bad"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertTrue(body["error"].orEmpty().isNotBlank())
    }
}
