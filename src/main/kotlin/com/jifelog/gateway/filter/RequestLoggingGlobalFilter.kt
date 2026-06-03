package com.jifelog.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class RequestLoggingGlobalFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val start = System.currentTimeMillis()

        return chain.filter(exchange)
            .doOnError { error ->
                val route = exchange.getAttribute<Route>(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)
                val req = exchange.request
                log.error(
                    "proxy error: {} {} -> route={} uri={} exception={} message={}",
                    req.method, req.uri.path, route?.id, route?.uri,
                    error.javaClass.simpleName, error.message,
                )
            }
            .doFinally {
                val route = exchange.getAttribute<Route>(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)
                val req = exchange.request
                val status = exchange.response.statusCode
                val took = System.currentTimeMillis() - start
                // 다운스트림으로 실제 프록시됐는지 여부. false면 게이트웨이가 자체 응답(예: 401)을 내려준 것.
                val routed = ServerWebExchangeUtils.isAlreadyRouted(exchange)
                val handledBy = if (routed) "downstream" else "gateway"

                log.info(
                    "{} {} -> route={} uri={} status={} handledBy={} ({}ms)",
                    req.method, req.uri.path, route?.id, route?.uri, status, handledBy, took,
                )
            }
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
