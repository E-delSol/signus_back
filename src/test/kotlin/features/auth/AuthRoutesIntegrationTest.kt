package features.auth

import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.configureApp
import com.pecadoartesano.features.auth.dto.AuthService
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
import support.testAppConfig

class AuthRoutesIntegrationTest {

    @Test
    fun `register returns created with token`() = testApplication {
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

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret","displayName":"User"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(true, response.bodyAsText().contains("token-register"))
    }

    @Test
    fun `login returns ok with token`() = testApplication {
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

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@test.com","password":"secret"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, response.bodyAsText().contains("token-login"))
    }
}
