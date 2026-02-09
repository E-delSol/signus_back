package com.pecadoartesano

import com.pecadoartesano.core.database.configureDatabase
import com.pecadoartesano.core.plugins.configureMonitoring
import com.pecadoartesano.core.plugins.configureRouting
import com.pecadoartesano.core.plugins.configureSecurity
import com.pecadoartesano.core.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureMonitoring()
    configureSerialization()
//    configureSockets()
    configureRouting()
    configureSecurity()
    configureDatabase()
}
