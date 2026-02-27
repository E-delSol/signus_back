package features.notification

import com.pecadoartesano.configureApp
import com.pecadoartesano.features.notification.dto.RealtimeNotificationService
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.takeFrom
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.ktor.websocket.WebSocketSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import support.createJwtToken
import support.createExpiredJwtToken
import support.createJwtTokenWithInvalidSignature
import support.testAppConfig
import org.koin.dsl.module as koinModule

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
    fun `given websocket session closes when connected then removes session`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        val trackingService = TrackingRealtimeNotificationService()
        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    koinModule {
                        single<RealtimeNotificationService> { trackingService }
                    }
                )
            )
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
        session.close()
        session.closeReason.await()

        // Then
        assertTrue(trackingService.registeredUserIds.contains("user-1"))
        withTimeout(1_000L) {
            while (!trackingService.removedUserIds.contains("user-1")) {
                yield()
            }
        }
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

    @Test
    fun `given expired token when websocket connects then closes with violated policy`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        val wsClient = client.config {
            install(WebSockets)
        }

        // When
        val token = createExpiredJwtToken("user-1", appConfig.jwt)
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

    private class TrackingRealtimeNotificationService : RealtimeNotificationService {
        val registeredUserIds = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val removedUserIds = java.util.concurrent.ConcurrentLinkedQueue<String>()

        override fun registerSession(userId: String, session: WebSocketSession) {
            registeredUserIds.add(userId)
        }

        override fun removeSession(userId: String) {
            removedUserIds.add(userId)
        }

        override suspend fun notifyPartnerStatusChanged(
            targetUserId: String,
            event: com.pecadoartesano.features.notification.dto.PartnerStatusChangedEvent
        ): Boolean = true
    }
}
