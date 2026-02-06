package com.pecadoartesano

import com.pecadoartesano.database.configureDatabase
import com.pecadoartesano.plugins.configureMonitoring
import com.pecadoartesano.plugins.configureRouting
import com.pecadoartesano.plugins.configureSecurity
import com.pecadoartesano.plugins.configureSerialization
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
