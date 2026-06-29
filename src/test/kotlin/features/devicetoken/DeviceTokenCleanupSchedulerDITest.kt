package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.core.config.DatabaseConfig
import com.pecadoartesano.core.config.FcmConfig
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.core.config.TokenCleanupConfig
import com.pecadoartesano.core.di.appModules
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import io.mockk.mockk
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertNotNull

class DeviceTokenCleanupSchedulerDITest {

    @Test
    fun `given appModules with tokenCleanupConfig then scheduler is resolvable`() {
        // Given
        val mockRepo = mockk<DeviceTokenRepositoryPort>(relaxed = true)
        val config = TokenCleanupConfig(staleDays = 30)

        val testConfig = AppConfig(
            jwt = JwtConfig(
                secret = "test", issuer = "test", audience = "test",
                realm = "test", accessTokenExpiration = 60000, refreshTokenExpiration = 604800000
            ),
            database = DatabaseConfig(
                host = "localhost", port = 5432, name = "test", user = "test", password = "test"
            ),
            fcm = FcmConfig(serverKey = "test-key"),
            tokenCleanup = config
        )

        val koinApp = koinApplication {
            modules(appModules(testConfig) + module {
                single<DeviceTokenRepositoryPort> { mockRepo }
            })
        }

        // When
        val scheduler = koinApp.koin.get<DeviceTokenCleanupScheduler>()

        // Then
        assertNotNull(scheduler)
    }
}
