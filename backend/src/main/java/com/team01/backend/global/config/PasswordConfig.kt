package com.team01.backend.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * [과제] 비밀번호 암호화를 위한 PasswordEncoder 설정입니다.
 * 보안 표준인 BCrypt 해시 함수를 사용하여 비밀번호를 안전하게 저장합니다.
 */
@Configuration
class PasswordConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(8)
    }
}