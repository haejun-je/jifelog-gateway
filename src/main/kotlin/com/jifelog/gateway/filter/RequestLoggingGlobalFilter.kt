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

        return chain.filter(exchange).doFinally {
            val route = exchange.getAttribute<Route>(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)
            val req = exchange.request
            val status = exchange.response.statusCode
            val took = System.currentTimeMillis() - start

            log.info("{} {} -> route={} uri={} status={} ({}ms)", req.method, req.uri.path, route?.id, route?.uri, status, took)
        }
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
