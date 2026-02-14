package com.jifelog.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.http.HttpStatus
import org.springframework.session.ReactiveSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.Base64

@Component
class SessionToJwtGlobalFilter(
    private val sessionRepository: ReactiveSessionRepository<out Session>
) : GlobalFilter {
    private val sessionCookieName = "SESSION"

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain
    ): Mono<Void> {
        if (shouldSkip(exchange)) {
            return chain.filter(exchange)
        }

        val cookieSessionId = extractSessionId(exchange) ?: return unauthorized(exchange)
        val sessionId = String(Base64.getUrlDecoder().decode(cookieSessionId), Charsets.UTF_8)

        sessionRepository.findById(sessionId)
            .doOnNext { session ->
                println("SESSION ID: ${session.id}")
                session.attributeNames.forEach { key ->
                    val value = session.getAttribute<Any>(key)
                    println(" - $key = $value")
                }
            }
            .doOnSuccess {
                if (it == null) {
                    println("SESSION NOT FOUND")
                }
            }
            .subscribe()

        return chain.filter(exchange)
    }

    private fun extractSessionId(exchange: ServerWebExchange): String? =
        exchange.request.cookies[sessionCookieName]?.firstOrNull()?.value

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    private fun shouldSkip(exchange: ServerWebExchange): Boolean {
        val path = exchange.request.uri.path
        val method = exchange.request.method.name()

        return method == "POST" && path == "/login"
    }
}