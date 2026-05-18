package com.team01.backend.config

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

/** 테스트에서 Redis 서버 없이 기동하기 위한 Mock */
@TestConfiguration
@Profile("test")
class TestRedisConfig {

    @Bean
    @Primary
    fun redisTemplate(): RedisTemplate<String, String> {
        val template = mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        val ops = mock(ValueOperations::class.java) as ValueOperations<String, String>
        `when`(template.opsForValue()).thenReturn(ops)
        return template
    }
}
