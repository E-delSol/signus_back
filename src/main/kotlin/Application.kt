package com.pecadoartesano

import com.pecadoartesano.core.config.loadConfig
import com.pecadoartesano.core.database.configureDatabase
import com.pecadoartesano.core.plugins.configureMonitoring
import com.pecadoartesano.core.plugins.configureRouting
import com.pecadoartesano.core.plugins.configureSecurity
import com.pecadoartesano.core.plugins.configureSerialization
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.AuthServiceImpl
import com.pecadoartesano.features.auth.dto.AuthService
import com.pecadoartesano.features.user.UserRepository
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val appConfig = loadConfig()

    configureMonitoring()
    configureSerialization()

    configureDatabase(appConfig.database)

    val jwtService = JwtService(appConfig.jwt)

    val authService: AuthService = AuthServiceImpl(
        userRepository = UserRepository(),
        passwordService = PasswordService(),
        jwtService = jwtService
    )

//    configureSockets()
    configureSecurity(appConfig.jwt)
    configureRouting(authService)
}
