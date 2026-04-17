package com.pecadoartesano.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.features.user.User
import java.util.Date

class JwtService(
    private val config: JwtConfig
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generateToken(user: User): String =
        generateAccessToken(user.id)

    fun generateAccessToken(userId: String): String {
        val now = System.currentTimeMillis()
        val exp = Date(now + config.accessTokenExpiration)
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("userId", userId)
            .withExpiresAt(exp)
            .sign(algorithm)
    }
}
