package com.pecadoartesano.core.config

data class AppConfig(
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val fcm: FcmConfig
)

data class FcmConfig(
    val serverKey: String
)

fun loadConfig(): AppConfig {
    val jwtConfig = JwtConfig(
        secret = System.getenv("JWT_SECRET") ?: error("secret property not set"),
        issuer = System.getenv("JWT_ISSUER") ?: error("issuer property not set"),
        audience = System.getenv("JWT_AUDIENCE") ?: error("audience property not set"),
        realm = System.getenv("JWT_REALM") ?: error("realm property not set"),
        accessTokenExpiration = System.getenv("JWT_ACCESS_EXPIRATION_TIME")?.toLongOrNull()
            ?: error("access token expiration property not set or invalid"),
        refreshTokenExpiration = System.getenv("JWT_REFRESH_EXPIRATION_TIME")?.toLongOrNull()
            ?: error("refresh token expiration property not set or invalid")
    )

    val databaseConfig = DatabaseConfig(
        host = System.getenv("DB_HOST") ?: error("dbHost property not set"),
        port = System.getenv("DB_PORT")?.toIntOrNull() ?: error("dbPort property not set or invalid"),
        name = System.getenv("DB_NAME") ?: error("dbName property not set"),
        user = System.getenv("DB_USER") ?: error("dbUser property not set"),
        password = System.getenv("DB_PASSWORD") ?: error("dbPassword property not set")
    )

    val fcmConfig = FcmConfig(
        serverKey = System.getenv("FCM_SERVER_KEY") ?: error("fcmServerKey property not set")
    )

    return AppConfig(
        jwt = jwtConfig,
        database = databaseConfig,
        fcm = fcmConfig
    )
}
