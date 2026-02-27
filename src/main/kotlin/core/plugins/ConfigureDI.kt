package com.pecadoartesano.core.plugins

import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.core.di.appModules
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDI(
    appConfig: AppConfig,
    overrideModules: List<Module> = emptyList()
) {
    val modules = appModules(appConfig) + overrideModules
    install(Koin) {
        slf4jLogger()
        modules(modules)
    }
}
