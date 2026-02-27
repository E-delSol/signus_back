package com.pecadoartesano

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import com.pecadoartesano.configureApp
import support.testAppConfig

class ApplicationTest {

    @Test
    fun `given test config when application starts then responds not found on root`() = testApplication {
        // Given
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }

        // When
        val response = client.get("/")

        // Then
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

}
