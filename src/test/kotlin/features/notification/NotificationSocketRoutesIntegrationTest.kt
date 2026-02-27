package features.notification

import com.pecadoartesano.configureApp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.takeFrom
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import support.createJwtToken
import support.createJwtTokenWithInvalidSignature
import support.testAppConfig

class NotificationSocketRoutesIntegrationTest {

    @Test
    fun `given missing token when websocket connects then closes with violated policy`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val wsClient = client.config {
            install(WebSockets)
        }

        // When
        val session = wsClient.webSocketSession {
            url {
                takeFrom("ws://localhost/ws")
            }
        }

        // Then
        val closeReason = session.closeReason.await()
        assertNotNull(closeReason)
        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, closeReason.code)
        assertEquals("Missing JWT token", closeReason.message)
    }

    @Test
    fun `given valid token when websocket connects then session is established`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val wsClient = client.config {
            install(WebSockets)
        }

        // When
        val token = createJwtToken("user-1", appConfig.jwt)
        val session = wsClient.webSocketSession {
            url {
                takeFrom("ws://localhost/ws")
                parameters.append("token", token)
            }
        }

        // Then
        session.close()
        val closeReason = session.closeReason.await()
        assertNotNull(closeReason)
    }

    @Test
    fun `given invalid token when websocket connects then closes with violated policy`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val wsClient = client.config {
            install(WebSockets)
        }

        // When
        val token = createJwtTokenWithInvalidSignature("user-1", appConfig.jwt)
        val session = wsClient.webSocketSession {
            url {
                takeFrom("ws://localhost/ws")
                parameters.append("token", token)
            }
        }

        // Then
        val closeReason = session.closeReason.await()
        assertNotNull(closeReason)
        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, closeReason.code)
        assertEquals("Invalid JWT token", closeReason.message)
    }
}
