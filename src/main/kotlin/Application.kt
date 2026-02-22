package com.pecadoartesano

import com.pecadoartesano.core.config.loadConfig
import com.pecadoartesano.core.database.configureDatabase
import com.pecadoartesano.core.plugins.configureMonitoring
import com.pecadoartesano.core.plugins.configureRouting
import com.pecadoartesano.core.plugins.configureSecurity
import com.pecadoartesano.core.plugins.configureSerialization
import com.pecadoartesano.core.plugins.configureSockets
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.AuthServiceImpl
import com.pecadoartesano.features.auth.dto.AuthService
import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.notification.RealtimeNotificationService
import com.pecadoartesano.features.notification.providers.FcmPushProvider
import com.pecadoartesano.features.semaphore.SemaphoreRepository
import com.pecadoartesano.features.semaphore.StatusService
import com.pecadoartesano.features.user.UserRepository
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val appConfig = loadConfig()

    configureMonitoring()
    configureSerialization()
    configureDatabase(appConfig.database)

    val userRepository = UserRepository()
    val jwtService = JwtService(appConfig.jwt)

    val authService: AuthService = AuthServiceImpl(
        userRepository = userRepository,
        passwordService = PasswordService(),
        jwtService = jwtService
    )

    val realtimeNotificationService = RealtimeNotificationService()
    val pushProvider = FcmPushProvider(serverKey = appConfig.fcm.serverKey)
    val notificationOrchestrator = NotificationOrchestrator(
        userRepository = userRepository,
        realtimeNotificationService = realtimeNotificationService,
        pushProvider = pushProvider
    )

    val statusService = StatusService(
        semaphoreRepository = SemaphoreRepository(),
        notificationOrchestrator = notificationOrchestrator
    )

    configureSecurity(appConfig.jwt)
    configureSockets(appConfig.jwt, realtimeNotificationService)
    configureRouting(authService, statusService)
}
