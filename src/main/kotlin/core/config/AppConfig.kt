package com.pecadoartesano.core.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

data class AppConfig(
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val fcm: FcmConfig,
    val tokenCleanup: TokenCleanupConfig = TokenCleanupConfig()
)

data class TokenCleanupConfig(
    val staleDays: Int = 30
)

data class FcmConfig(
    val serviceAccountJson: String?
) {
    /** Parse project_id from the service account JSON — single source of truth. */
    val projectId: String? by lazy {
        serviceAccountJson?.let { json ->
            val parser = Json { ignoreUnknownKeys = true }
            val sa = parser.decodeFromString<ServiceAccountJson>(json)
            sa.project_id
        }
    }
}

@Serializable
private data class ServiceAccountJson(val project_id: String)

fun resolveServiceAccountJson(): String? {
    val path = System.getenv("FCM_SERVICE_ACCOUNT_PATH")
    if (path != null) {
        val file = File(path)
        if (!file.exists()) error("FCM_SERVICE_ACCOUNT_PATH: file not found at $path")
        if (!file.isFile) error("FCM_SERVICE_ACCOUNT_PATH: not a regular file at $path")
        if (!file.canRead()) error("FCM_SERVICE_ACCOUNT_PATH: file not readable at $path")
        val content = file.readText()
        if (content.isBlank()) error("FCM_SERVICE_ACCOUNT_PATH: file is empty at $path")
        try {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<ServiceAccountJson>(content)
        } catch (e: Exception) {
            error("FCM_SERVICE_ACCOUNT_PATH: invalid JSON in $path: ${e.message}")
        }
        return content
    }
    return System.getenv("FCM_SERVICE_ACCOUNT_JSON")
}

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
        serviceAccountJson = resolveServiceAccountJson()
    )

    val tokenCleanupConfig = TokenCleanupConfig(
        staleDays = System.getenv("TOKEN_STALE_DAYS")?.toIntOrNull() ?: 30
    )

    return AppConfig(
        jwt = jwtConfig,
        database = databaseConfig,
        fcm = fcmConfig,
        tokenCleanup = tokenCleanupConfig
    )
}
