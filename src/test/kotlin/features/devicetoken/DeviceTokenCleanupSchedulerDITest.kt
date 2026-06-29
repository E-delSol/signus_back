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
            fcm = FcmConfig(
                projectId = "test-project",
                serviceAccountJson = """{"type":"service_account","project_id":"test","private_key_id":"test","private_key":"-----BEGIN PRIVATE KEY-----\nMIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEA\n-----END PRIVATE KEY-----\n","client_email":"test@test.iam.gserviceaccount.com","client_id":"123","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"https://www.googleapis.com/robot/v1/metadata/x509/test@test.iam.gserviceaccount.com"}"""
            ),
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
