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
    fun testApplicationBootstrapsWithTestConfig() = testApplication {
        val appConfig = testAppConfig()
        application {
            configureApp(appConfig = appConfig, startDatabase = false)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

}
