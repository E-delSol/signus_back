package com.pecadoartesano.core.config



data class AppConfig(
    val jwt: JwtConfig,
    val database: DatabaseConfig
)

fun loadConfig(): AppConfig {
    val jwtConfig = JwtConfig(
        secret = System.getenv("JWT_SECRET") ?: error("secret property not set"),
        issuer = System.getenv("JWT_ISSUER") ?: error("issuer property not set"),
        audience = System.getenv("JWT_AUDIENCE") ?: error("audience property not set"),
        realm = System.getenv("JWT_REALM") ?: error("realm property not set"),
        expiration = System.getenv("JWT_EXPIRATION_TIME")?.toLongOrNull() ?: error("expiration property not set or invalid")
    )

    val databaseConfig = DatabaseConfig(
        host = System.getenv("DB_HOST") ?: error("dbHost property not set"),
        port = System.getenv("DB_PORT")?.toIntOrNull() ?: error("dbPort property not set or invalid"),
        name = System.getenv("DB_NAME") ?: error("dbName property not set"),
        user = System.getenv("DB_USER") ?: error("dbUser property not set"),
        password = System.getenv("DB_PASSWORD") ?: error("dbPassword property not set")
    )

    return AppConfig(
        jwt = jwtConfig,
        database = databaseConfig
    )
}
