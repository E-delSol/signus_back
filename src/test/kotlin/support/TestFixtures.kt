package support

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.core.config.DatabaseConfig
import com.pecadoartesano.core.config.FcmConfig
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.core.config.TokenCleanupConfig
import java.util.Date
import kotlinx.serialization.json.Json

fun testAppConfig(): AppConfig =
    AppConfig(
        jwt = JwtConfig(
            secret = "test-secret",
            issuer = "test-issuer",
            audience = "test-audience",
            realm = "test-realm",
            accessTokenExpiration = 60_000L,
            refreshTokenExpiration = 604_800_000L
        ),
        database = DatabaseConfig(
            host = "localhost",
            port = 5432,
            name = "testdb",
            user = "test",
            password = "test"
        ),
        fcm = FcmConfig(
            serviceAccountJson = """{"type":"service_account","project_id":"test-project","private_key_id":"test","private_key":"-----BEGIN PRIVATE KEY-----\nMIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEA\n-----END PRIVATE KEY-----\n","client_email":"test@test.iam.gserviceaccount.com","client_id":"123","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"https://www.googleapis.com/robot/v1/metadata/x509/test@test.iam.gserviceaccount.com"}"""
        ),
        tokenCleanup = TokenCleanupConfig(staleDays = 30)
    )

val testJson: Json = Json { ignoreUnknownKeys = true }

fun createJwtToken(userId: String, jwtConfig: JwtConfig): String {
    val now = System.currentTimeMillis()
    return JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(now + jwtConfig.accessTokenExpiration))
        .sign(Algorithm.HMAC256(jwtConfig.secret))
}

inline fun <reified T> decodeJson(body: String): T = testJson.decodeFromString(body)

fun createExpiredJwtToken(userId: String, jwtConfig: JwtConfig): String {
    val now = System.currentTimeMillis()
    return JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(now - 1_000L))
        .sign(Algorithm.HMAC256(jwtConfig.secret))
}

fun createJwtTokenWithoutUserId(jwtConfig: JwtConfig): String {
    val now = System.currentTimeMillis()
    return JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withExpiresAt(Date(now + jwtConfig.accessTokenExpiration))
        .sign(Algorithm.HMAC256(jwtConfig.secret))
}

fun createJwtTokenWithInvalidSignature(userId: String, jwtConfig: JwtConfig): String {
    val now = System.currentTimeMillis()
    return JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(now + jwtConfig.accessTokenExpiration))
        .sign(Algorithm.HMAC256("invalid-secret"))
}
