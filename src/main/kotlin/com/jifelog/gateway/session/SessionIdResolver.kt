package com.jifelog.gateway.session

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.Base64

@Component
class SessionIdResolver {
    private val sessionCookieName = "SESSION"

    fun resolve(exchange: ServerWebExchange): String? {
        val cookieValue = exchange.request.cookies[sessionCookieName]?.firstOrNull()?.value ?: return null
        return decode(cookieValue)
    }

    private fun decode(cookieSessionId: String): String? =
        runCatching {
            String(Base64.getUrlDecoder().decode(cookieSessionId), Charsets.UTF_8)
        }.getOrNull()
}