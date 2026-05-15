package com.team01.backend.global.config

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * [과제] 로컬 개발 환경 전용 SMTP 가상 서버 설정입니다.
 * 실제 메일 서버 없이도 이메일 발송 기능을 테스트할 수 있게 해줍니다.
 */
@Configuration
class MailConfig {

    @Bean
    @Profile("local")
    fun greenMail(): GreenMail {
        // 3025 포트에서 SMTP 가상 서버를 가동합니다.
        val greenMail = GreenMail(ServerSetup(3025, null, "smtp"))
        greenMail.start()
        return greenMail
    }
}