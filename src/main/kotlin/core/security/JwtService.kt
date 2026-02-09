package com.pecadoartesano.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.features.user.User
import com.typesafe.config.ConfigFactory
import java.util.Date

class JwtService {
    private val config = ConfigFactory.load()
    private val issuer = config.getString("jwt.issuer")
    private val audience = config.getString("jwt.audience")
    private val secret = config.getString("jwt.secret")
    private val expiration = config.getString("jwt.expiration")

    private val algorithm = Algorithm.HMAC256(secret)

        fun generateToken(user: User): String {
            val now = System.currentTimeMillis()
            val exp = Date(now + expiration.toLong())
            return JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim("userId", user.id)
                .withExpiresAt(exp)
                .sign(algorithm)
        }
}