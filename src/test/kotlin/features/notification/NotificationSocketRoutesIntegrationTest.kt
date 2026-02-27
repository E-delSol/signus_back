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
import support.testAppConfig

class NotificationSocketRoutesIntegrationTest {

    @Test
    fun `websocket closes with violated policy when token is missing`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val wsClient = client.config {
            install(WebSockets)
        }

        val session = wsClient.webSocketSession {
            url {
                takeFrom("ws://localhost/ws")
            }
        }

        val closeReason = session.closeReason.await()
        assertNotNull(closeReason)
        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, closeReason.code)
        assertEquals("Missing JWT token", closeReason.message)
    }

    @Test
    fun `websocket connects with valid token`() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val wsClient = client.config {
            install(WebSockets)
        }

        val token = createJwtToken("user-1", appConfig.jwt)
        val session = wsClient.webSocketSession {
            url {
                takeFrom("ws://localhost/ws")
                parameters.append("token", token)
            }
        }

        session.close()
        val closeReason = session.closeReason.await()
        assertNotNull(closeReason)
    }
}
