package com.jifelog.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["com.jifelog.gateway.jwt"])
class JifelogGatewayApplication

fun main(args: Array<String>) {
	runApplication<JifelogGatewayApplication>(*args)
}
