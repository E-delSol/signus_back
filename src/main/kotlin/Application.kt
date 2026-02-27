package com.pecadoartesano

import com.pecadoartesano.core.config.loadConfig
import com.pecadoartesano.core.database.configureDatabase
import com.pecadoartesano.core.plugins.configureDI
import com.pecadoartesano.core.plugins.configureMonitoring
import com.pecadoartesano.core.plugins.configureRouting
import com.pecadoartesano.core.plugins.configureSecurity
import com.pecadoartesano.core.plugins.configureSerialization
import com.pecadoartesano.core.plugins.configureSockets
import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.features.auth.dto.AuthService
import com.pecadoartesano.features.notification.dto.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.StatusService
import io.ktor.server.application.Application
import org.koin.core.module.Module
import org.koin.ktor.ext.get

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureApp(loadConfig())
}

internal fun Application.configureApp(
    appConfig: AppConfig,
    startDatabase: Boolean = true,
    overrideModules: List<Module> = emptyList()
) {

    configureMonitoring()
    configureSerialization()
    if (startDatabase) {
        configureDatabase(appConfig.database)
    }
    configureDI(appConfig, overrideModules)

    val authService: AuthService = get()
    val statusService: StatusService = get()
    val realtimeNotificationService: RealtimeNotificationService = get()

    configureSecurity(appConfig.jwt)
    configureSockets()
    configureRouting(authService, statusService, appConfig.jwt, realtimeNotificationService)
}
