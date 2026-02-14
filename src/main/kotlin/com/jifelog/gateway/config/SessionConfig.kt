package com.jifelog.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisIndexedWebSession
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableRedisIndexedWebSession(
    maxInactiveIntervalInSeconds = 60 * 30,
    redisNamespace = "jifelog:auth"
)
class SessionConfig {
    @Bean
    fun springSessionDefaultRedisSerializer(jsonMapper: JsonMapper): RedisSerializer<Any> {
        return JacksonJsonRedisSerializer(jsonMapper, Any::class.java)
    }
}