package com.jifelog.gateway.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.session.Session
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtGenerator(
    private val props: JwtProperties,
) {
    private val userSessionAttrName = "USER_SESSION"
    private val algo: Algorithm = Algorithm.HMAC256(props.secret)

    init {
        require(props.secret.isNotBlank()) { "gateway.jifelog-jwt.secret must not be blank" }
    }

    fun headerName(): String = props.headerName

    fun generate(session: Session): String {
        val now = Date()
        val expiresAt = Date(now.time + (props.ttlSeconds.coerceAtLeast(1) * 1000))

        val jwtBuilder = JWT.create()
            .withIssuer(props.issuer)
            .withAudience(props.audience)
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(session.id)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)

        val userSession = session.getAttribute<LinkedHashMap<String, String>>(userSessionAttrName)

        userSession.forEach { (k, v) ->
            when (k) {
                "id" -> jwtBuilder.withClaim("id", v)
                "username" -> { jwtBuilder.withClaim("un", v) }
                "email" -> { jwtBuilder.withClaim("em", v) }
            }
        }

        return jwtBuilder.sign(algo)
    }
}