package com.jifelog.gateway.filter

import com.jifelog.gateway.jwt.JwtGenerator
import com.jifelog.gateway.session.SessionIdResolver
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.http.HttpStatus
import org.springframework.session.ReactiveSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Component
import org.springframework.web.cors.reactive.CorsUtils
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SessionToJwtGlobalFilter(
    private val sessionRepository: ReactiveSessionRepository<out Session>,
    private val sessionIdResolver: SessionIdResolver,
    private val jwtGenerator: JwtGenerator,
) : GlobalFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        // 내부 JWT 헤더 제거(클라이언트 주입 방지)
        val sanitizedExchange = exchange.mutate()
            .request(
                exchange.request.mutate()
                    .headers { it.remove(jwtGenerator.headerName()) }
                    .build()
            )
            .build()

        if (shouldSkip(sanitizedExchange)) {
            return chain.filter(sanitizedExchange)
        }

        val sessionId = sessionIdResolver.resolve(sanitizedExchange) ?: return unauthorized(sanitizedExchange)

        return sessionRepository.findById(sessionId)
            .flatMap { session ->
                val internalJwt = jwtGenerator.generate(session)

                val requestWithJwt = sanitizedExchange.request.mutate()
                    .headers { it.set(jwtGenerator.headerName(), internalJwt) }
                    .build()

                chain.filter(
                    sanitizedExchange.mutate().request(requestWithJwt).build()
                )
            }
            .switchIfEmpty(unauthorized(sanitizedExchange))
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    private fun shouldSkip(exchange: ServerWebExchange): Boolean {
        val path = exchange.request.uri.path
        val method = exchange.request.method.name()

        return CorsUtils.isPreFlightRequest(exchange.request) ||
                (method == "POST" && path == "/login") ||
                (method == "POST" && path == "/signup") ||
                path.startsWith("/signup/")
    }
}
