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
import com.pecadoartesano.features.auth.ports.AuthService
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenService
import com.pecadoartesano.features.linking.ports.LinkingService
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.semaphore.ports.StatusService
import com.pecadoartesano.features.user.ports.UserService
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
    val linkingService: LinkingService = get()
    val userService: UserService = get()
    val deviceTokenService: DeviceTokenService = get()

    configureSecurity(appConfig.jwt)
    configureSockets()
    configureRouting(
        authService = authService,
        statusService = statusService,
        jwtConfig = appConfig.jwt,
        realtimeNotificationService = realtimeNotificationService,
        linkingService = linkingService,
        userService = userService,
        deviceTokenService = deviceTokenService
    )
}
