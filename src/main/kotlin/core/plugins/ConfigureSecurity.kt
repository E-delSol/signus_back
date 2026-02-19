package com.pecadoartesano.core.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pecadoartesano.core.config.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    authentication {
        jwt {
            realm = jwtConfig.realm

            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtConfig.audience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}


//fun Application.configureSecurity(jwtConfig: JwtConfig) {
//    authentication {
//        jwt() {
//            val environment = this@configureSecurity.environment
//            val jwtAudience = environment.config.property(jwtConfig.audience).getString()
//            realm = environment.config.property(jwtConfig.realm).getString()
//            verifier(
//                JWT
//                    .require(Algorithm.HMAC256(jwtConfig.secret))
//                    .withAudience(jwtAudience)
//                    .withIssuer(environment.config.property(jwtConfig.issuer).getString())
//                    .build()
//            )
//            validate { credential ->
//                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
//            }
//        }
//    }
//}