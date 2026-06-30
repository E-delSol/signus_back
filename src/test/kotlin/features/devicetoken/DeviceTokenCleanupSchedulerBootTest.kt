package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.configureApp
import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.core.config.DatabaseConfig
import com.pecadoartesano.core.config.FcmConfig
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.core.config.TokenCleanupConfig
import com.pecadoartesano.core.di.appModules
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import support.testAppConfig

class DeviceTokenCleanupSchedulerBootTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given application start when configureApp then scheduler does not crash`() = testApplication {
        val appConfig = testAppConfig()

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false
            )
        }
    }

    @Test
    fun `given application start when configureApp then scheduler starts cleanup via override`() = testApplication {
        val repository = mockk<DeviceTokenRepositoryPort>(relaxed = true)
        every { repository.findActiveTokensOlderThan(any()) } returns emptyList()

        val appConfig = testAppConfig()

        application {
            configureApp(
                appConfig = appConfig,
                startDatabase = false,
                overrideModules = listOf(
                    module {
                        single<DeviceTokenRepositoryPort> { repository }
                    }
                )
            )
        }
    }
}
