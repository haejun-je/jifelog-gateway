package com.jifelog.gateway.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jifelog.jwt")
data class JwtProperties(
    val secret: String,
    val ttlSeconds: Long,
    val issuer: String,
    val audience: String,
    val headerName: String,
)
