package support

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.core.config.DatabaseConfig
import com.pecadoartesano.core.config.FcmConfig
import com.pecadoartesano.core.config.JwtConfig
import java.util.Date

fun testAppConfig(): AppConfig =
    AppConfig(
        jwt = JwtConfig(
            secret = "test-secret",
            issuer = "test-issuer",
            audience = "test-audience",
            realm = "test-realm",
            expiration = 60_000L
        ),
        database = DatabaseConfig(
            host = "localhost",
            port = 5432,
            name = "testdb",
            user = "test",
            password = "test"
        ),
        fcm = FcmConfig(serverKey = "test-fcm-key")
    )

fun createJwtToken(userId: String, jwtConfig: JwtConfig): String {
    val now = System.currentTimeMillis()
    return JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(now + jwtConfig.expiration))
        .sign(Algorithm.HMAC256(jwtConfig.secret))
}
