package features.semaphore

import com.pecadoartesano.configureApp
import com.pecadoartesano.features.semaphore.Semaphore
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.semaphore.ports.StatusService
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module as koinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import support.createJwtToken
import support.createExpiredJwtToken
import support.createJwtTokenWithInvalidSignature
import support.createJwtTokenWithoutUserId
import support.decodeJson
import support.testAppConfig

class StatusRoutesIntegrationTest {

    @Test
    fun `given valid token when patch status then returns ok with updated status`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val statusService = mockk<StatusService>()
        val expected = Semaphore(
            status = SemaphoreStatus.BUSY,
            userId = "user-1",
            expiration = null,
            duration = null
        )
        coEvery { statusService.updateStatus("user-1", SemaphoreStatus.BUSY) } returns expected

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single { statusService }
                    }
                )
            )
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val body = decodeJson<Semaphore>(response.bodyAsText())
        assertEquals(SemaphoreStatus.BUSY, body.status)
        assertEquals("user-1", body.userId)
    }

    @Test
    fun `given missing token when patch status then returns unauthorized`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val response = client.patch("/status") {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given invalid token when patch status then returns unauthorized`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val token = createJwtTokenWithInvalidSignature("user-1", appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given expired token when patch status then returns unauthorized`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val token = createExpiredJwtToken("user-1", appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given token without userId claim when patch status then returns unauthorized`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val token = createJwtTokenWithoutUserId(appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given malformed json when patch status then returns bad request`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{""")
        }

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `given invalid enum when patch status then returns bad request`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"INVALID"}""")
        }

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
