package integration.di

import com.pecadoartesano.configureApp
import com.pecadoartesano.features.auth.dto.AuthService
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import support.decodeJson
import support.testAppConfig
import com.pecadoartesano.features.auth.dto.TokenResponse
import org.koin.dsl.module as koinModule

class KoinWiringIntegrationTest {

    @Test
    fun `given test app config when application starts then auth route responds`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret"}""")
        }

        // Then
        assertTrue(
            response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Conflict,
            "Expected Unauthorized or Conflict but was ${response.status}"
        )
    }

    @Test
    fun `given override module when auth route is called then override is used`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val fakeAuthService = object : AuthService {
            override fun register(email: String, rawPassword: String, displayName: String?): String = "token-register"
            override fun login(email: String, rawPassword: String): String = "token-override"
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
        assertEquals("token-override", body.accessToken)
    }
}
