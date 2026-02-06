package com.pecadoartesano.database

import com.pecadoartesano.config.DotEnvConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

fun configureDatabase() {
    initDB()
}

private fun initDB() {

    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://${DotEnvConfig.dbHost}:${DotEnvConfig.dbPort}/${DotEnvConfig.dbName}"
        username = DotEnvConfig.dbUser
        password = DotEnvConfig.dbPassword
    }

    HikariDataSource(config).also { dataSource ->
        runFlyway(dataSource)
        Database.connect(dataSource)
    }

}

private fun runFlyway(dataSource: DataSource) {
    val flyway = Flyway.configure().dataSource(dataSource).load()
    try {
        flyway.info()
        flyway.migrate()
    } catch (e: Exception) {
        throw e
    }
}
