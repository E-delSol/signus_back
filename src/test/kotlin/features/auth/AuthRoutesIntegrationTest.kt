package features.auth

import com.pecadoartesano.configureApp
import com.pecadoartesano.core.exceptions.EmailAlreadyExistsException
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.features.auth.AuthSessionTokens
import com.pecadoartesano.features.auth.InvalidRefreshTokenException
import com.pecadoartesano.features.auth.RefreshTokenExpiredException
import com.pecadoartesano.features.auth.RefreshTokenRevokedException
import com.pecadoartesano.features.auth.dto.AuthSessionResponse
import com.pecadoartesano.features.auth.dto.RefreshSessionResponse
import com.pecadoartesano.features.auth.ports.AuthService
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.semaphore.UserState
import com.pecadoartesano.features.user.ports.UserService
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.koin.dsl.module as koinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import support.createJwtToken
import support.decodeJson
import support.testAppConfig

class AuthRoutesIntegrationTest {

    @Test
    fun `given valid register request when post then returns created with access and refresh tokens`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            registerResult = AuthSessionTokens("access-register", "refresh-register")
        )

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

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret","displayName":"User"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = decodeJson<AuthSessionResponse>(response.bodyAsText())
        assertEquals("access-register", body.accessToken)
        assertEquals("refresh-register", body.refreshToken)
    }

    @Test
    fun `given valid login request when post then returns ok with access and refresh tokens`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            loginResult = AuthSessionTokens("access-login", "refresh-login")
        )

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

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = decodeJson<AuthSessionResponse>(response.bodyAsText())
        assertEquals("access-login", body.accessToken)
        assertEquals("refresh-login", body.refreshToken)
    }

    @Test
    fun `given valid refresh token when post refresh then returns new access token`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(refreshResult = "new-access-token")

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

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"valid-refresh"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = decodeJson<RefreshSessionResponse>(response.bodyAsText())
        assertEquals("new-access-token", body.accessToken)
    }

    @Test
    fun `given token returned by refresh when calling protected route then returns ok`() = testApplication {
        val appConfig = testAppConfig()
        val refreshedAccessToken = JwtService(appConfig.jwt).generateAccessToken("user-1")
        val fakeAuthService = fakeAuthService(refreshResult = refreshedAccessToken)
        val fakeUserService = object : UserService {
            override fun register(email: String, rawPassword: String, displayName: String?) =
                throw UnsupportedOperationException("Not used in this test")

            override fun getCurrentUser(userId: String): UserState =
                UserState(
                    id = userId,
                    partnerId = null,
                    status = SemaphoreStatus.AVAILABLE,
                    statusExpiration = null,
                    statusDuration = null
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
                        single<AuthService> { fakeAuthService }
                        single<UserService> { fakeUserService }
                    }
                )
            )
        }

        val refreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"valid-refresh"}""")
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshBody = decodeJson<RefreshSessionResponse>(refreshResponse.bodyAsText())

        val protectedResponse = client.get("/me") {
            header(HttpHeaders.Authorization, "Bearer ${refreshBody.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, protectedResponse.status)
    }

    @Test
    fun `given invalid refresh token when post refresh then returns unauthorized`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            refreshException = InvalidRefreshTokenException("Refresh token is invalid.")
        )

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

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"bad-refresh"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertEquals("Refresh token is invalid.", body["error"])
    }

    @Test
    fun `given expired refresh token when post refresh then returns unauthorized`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            refreshException = RefreshTokenExpiredException("Refresh token has expired.")
        )

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

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"expired-refresh"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertEquals("Refresh token has expired.", body["error"])
    }

    @Test
    fun `given revoked refresh token when post refresh then returns unauthorized`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            refreshException = RefreshTokenRevokedException("Refresh token has been revoked.")
        )

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

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"revoked-refresh"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertEquals("Refresh token has been revoked.", body["error"])
    }

    @Test
    fun `given valid token and refresh token when post logout then returns no content`() = testApplication {
        val appConfig = testAppConfig()
        var capturedLogoutUserId: String? = null
        var capturedLogoutRefreshToken: String? = null
        val fakeAuthService = fakeAuthService(
            logoutBlock = { userId, refreshToken ->
                capturedLogoutUserId = userId
                capturedLogoutRefreshToken = refreshToken
            }
        )

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

        val accessToken = createJwtToken("user-1", appConfig.jwt)
        val response = client.post("/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"refresh-to-revoke"}""")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("user-1", capturedLogoutUserId)
        assertEquals("refresh-to-revoke", capturedLogoutRefreshToken)
    }

    @Test
    fun `given auth service throws illegal argument when register then returns bad request`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            registerException = IllegalArgumentException("Invalid data")
        )

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

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"","password":"","displayName":null}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertTrue(body["error"].orEmpty().isNotBlank())
    }

    @Test
    fun `given auth service throws email already exists when register then returns conflict`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            registerException = EmailAlreadyExistsException("Email already exists")
        )

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

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"existing@test.com","password":"secret","displayName":"User"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertEquals("Email already exists", body["error"])
    }

    @Test
    fun `given auth service throws generic exception when register then returns conflict`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            registerException = RuntimeException("Unexpected")
        )

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

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret","displayName":"User"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertTrue(body["error"].orEmpty().isNotBlank())
    }

    @Test
    fun `given auth service throws illegal argument when login then returns unauthorized`() = testApplication {
        val appConfig = testAppConfig()
        val fakeAuthService = fakeAuthService(
            loginException = IllegalArgumentException("Invalid credentials")
        )

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

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"bad"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = decodeJson<Map<String, String>>(response.bodyAsText())
        assertTrue(body["error"].orEmpty().isNotBlank())
    }

    @Test
    fun `given malformed json when register then returns bad request`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `given missing required fields when register then returns bad request`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `given malformed json when login then returns bad request`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `given missing required fields when login then returns bad request`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private fun fakeAuthService(
        registerResult: AuthSessionTokens = AuthSessionTokens("token-register", "refresh-register"),
        loginResult: AuthSessionTokens = AuthSessionTokens("token-login", "refresh-login"),
        refreshResult: String = "refreshed-access-token",
        registerException: Exception? = null,
        loginException: Exception? = null,
        refreshException: Exception? = null,
        logoutException: Exception? = null,
        logoutBlock: (String, String) -> Unit = { _, _ -> }
    ): AuthService =
        object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): AuthSessionTokens {
                registerException?.let { throw it }
                return registerResult
            }

            override fun login(email: String, rawPassword: String): AuthSessionTokens {
                loginException?.let { throw it }
                return loginResult
            }

            override fun refresh(refreshToken: String): String {
                refreshException?.let { throw it }
                return refreshResult
            }

            override fun logout(userId: String, refreshToken: String) {
                logoutException?.let { throw it }
                logoutBlock(userId, refreshToken)
            }
        }
}
