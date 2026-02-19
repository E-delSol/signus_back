package com.pecadoartesano.core.database

import com.pecadoartesano.core.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

fun configureDatabase(databaseConfig: DatabaseConfig) {
    initDB(databaseConfig)
}

private fun initDB(config: DatabaseConfig) {

    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
        username = config.user
        password = config.password
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
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
