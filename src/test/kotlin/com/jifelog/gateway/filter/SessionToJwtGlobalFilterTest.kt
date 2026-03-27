package com.jifelog.gateway.filter

import com.jifelog.gateway.jwt.JwtGenerator
import com.jifelog.gateway.jwt.JwtProperties
import com.jifelog.gateway.session.SessionIdResolver
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.session.MapSession
import org.springframework.session.ReactiveSessionRepository
import org.springframework.session.Session
import reactor.core.publisher.Mono
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionToJwtGlobalFilterTest {

    private val jwtGenerator = JwtGenerator(
        JwtProperties(
            secret = "test-secret",
            ttlSeconds = 60,
            issuer = "gateway.jifelog.com",
            audience = "jifelog-internal-services",
            headerName = "X-Jifelog-JWT",
        )
    )

    @Test
    fun `allows cors preflight without session`() {
        val filter = SessionToJwtGlobalFilter(
            sessionRepository = FakeSessionRepository(),
            sessionIdResolver = SessionIdResolver(),
            jwtGenerator = jwtGenerator,
        )
        var chainCalled = false
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.options("/auth/me")
                .header("Origin", "https://app.jifelog.com")
                .header("Access-Control-Request-Method", "GET")
                .build()
        )

        filter.filter(exchange,  {
            chainCalled = true
            Mono.empty()
        }).block()

        assertTrue(chainCalled)
    }

    @Test
    fun `allows login request without session`() {
        val filter = SessionToJwtGlobalFilter(
            sessionRepository = FakeSessionRepository(),
            sessionIdResolver = SessionIdResolver(),
            jwtGenerator = jwtGenerator,
        )
        var chainCalled = false
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/login").build()
        )

        filter.filter(exchange) {
            chainCalled = true
            Mono.empty()
        }.block()

        assertTrue(chainCalled)
    }

    @Test
    fun `allows signup root request without session`() {
        val filter = SessionToJwtGlobalFilter(
            sessionRepository = FakeSessionRepository(),
            sessionIdResolver = SessionIdResolver(),
            jwtGenerator = jwtGenerator,
        )
        var chainCalled = false
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/signup").build()
        )

        filter.filter(exchange) {
            chainCalled = true
            Mono.empty()
        }.block()

        assertTrue(chainCalled)
    }

    @Test
    fun `allows signup child request without session`() {
        val filter = SessionToJwtGlobalFilter(
            sessionRepository = FakeSessionRepository(),
            sessionIdResolver = SessionIdResolver(),
            jwtGenerator = jwtGenerator,
        )
        var chainCalled = false
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/signup/email/verify").build()
        )

        filter.filter(exchange) {
            chainCalled = true
            Mono.empty()
        }.block()

        assertTrue(chainCalled)
    }

    @Test
    fun `returns unauthorized when session does not exist`() {
        val filter = SessionToJwtGlobalFilter(
            sessionRepository = FakeSessionRepository(),
            sessionIdResolver = SessionIdResolver(),
            jwtGenerator = jwtGenerator,
        )
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/auth/me")
                .cookie(org.springframework.http.HttpCookie("SESSION", encodeSessionId("missing-session")))
                .build()
        )

        filter.filter(exchange) { Mono.empty() }.block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `injects internal jwt when session exists`() {
        val session = MapSession("session-123").apply {
            setAttribute(
                "USER_SESSION",
                linkedMapOf(
                    "id" to "1",
                    "username" to "tester",
                    "email" to "tester@jifelog.com",
                )
            )
        }
        val filter = SessionToJwtGlobalFilter(
            sessionRepository = FakeSessionRepository(session),
            sessionIdResolver = SessionIdResolver(),
            jwtGenerator = jwtGenerator,
        )
        var forwardedHeader: String? = null
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/auth/me")
                .cookie(org.springframework.http.HttpCookie("SESSION", encodeSessionId(session.id)))
                .build()
        )

        filter.filter(exchange) { forwardedExchange ->
            forwardedHeader = forwardedExchange.request.headers.getFirst("X-Jifelog-JWT")
            Mono.empty()
        }.block()

        assertTrue(!forwardedHeader.isNullOrBlank())
    }

    private fun encodeSessionId(sessionId: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(sessionId.toByteArray())

    private class FakeSessionRepository(
        private val session: Session? = null,
    ) : ReactiveSessionRepository<Session> {

        override fun createSession(): Mono<Session> = Mono.just(MapSession())

        override fun save(session: Session): Mono<Void> = Mono.empty()

        override fun findById(id: String): Mono<Session> =
            if (session != null && session.id == id) Mono.just(session) else Mono.empty()

        override fun deleteById(id: String): Mono<Void> = Mono.empty()
    }
}
