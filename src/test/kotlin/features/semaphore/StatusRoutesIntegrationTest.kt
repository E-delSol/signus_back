package features.semaphore

import com.pecadoartesano.configureApp
import com.pecadoartesano.features.semaphore.Semaphore
import com.pecadoartesano.features.semaphore.SemaphoreStatus
import com.pecadoartesano.features.semaphore.StatusService
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
import support.testAppConfig

class StatusRoutesIntegrationTest {

    @Test
    fun `patch status returns ok when token is valid`() = testApplication {
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

        val token = createJwtToken("user-1", appConfig.jwt)
        val response = client.patch("/status") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, response.bodyAsText().contains("BUSY"))
    }

    @Test
    fun `patch status returns unauthorized without token`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val response = client.patch("/status") {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"BUSY"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
