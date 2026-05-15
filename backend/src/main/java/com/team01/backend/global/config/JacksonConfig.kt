package com.team01.backend.global.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    /**
     * Kotlin Data Class의 역직렬화(JSON -> Object)를 지원하기 위한 빈 등록
     */
    @Bean
    fun kotlinModule(): KotlinModule {
        return KotlinModule.Builder().build()
    }
}