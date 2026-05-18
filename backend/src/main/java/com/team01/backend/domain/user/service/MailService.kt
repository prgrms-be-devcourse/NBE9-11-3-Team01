package com.team01.backend.domain.user.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

// 인증 코드와 만료 시간을 담는 데이터 클래스
private data class VerificationCode(
    val code: String,
    val issuedAt: LocalDateTime = LocalDateTime.now()
)

@Service
class MailService(private val mailSender: JavaMailSender) {

    // 멀티스레드 환경을 고려한 스레드 안전성 확보
    private val codeStore = ConcurrentHashMap<String, VerificationCode>()

    fun sendVerificationCode(email: String) {
        val code = ((Math.random() * 900000) + 100000).toInt().toString()
        
        // 메모리에 5분 기한으로 저장
        codeStore[email] = VerificationCode(code)

        val message = SimpleMailMessage().apply {
            setTo(email)
            setSubject("[과제] 본인 인증 코드")
            setText("인증 코드는 [$code] 입니다. 5분 내에 입력해 주세요.")
        }
        mailSender.send(message)
    }

    fun verifyCode(email: String, code: String): Boolean {
        val stored = codeStore[email] ?: return false
        
        // 1. 수동적 만료 검사
        if (LocalDateTime.now().isAfter(stored.issuedAt.plusMinutes(VERIFICATION_CODE_TTL_MINUTES))) {
            codeStore.remove(email)
            return false
        }
        
        if (stored.code != code) return false
        
        // 2. 인증 성공 시 보안을 위해 즉시 삭제 (1회용)
        codeStore.remove(email)
        return true
    }

    // 3. 능동적 정화: 1시간마다 확인되지 않은 만료 데이터 일괄 삭제
    @Scheduled(fixedRate = 3600000)
    fun cleanupStore() {
        val now = LocalDateTime.now()
        codeStore.entries.removeIf { entry -> entry.value.issuedAt.plusMinutes(VERIFICATION_CODE_TTL_MINUTES).isBefore(now) }
    }

    companion object {
        private const val VERIFICATION_CODE_TTL_MINUTES = 5L
    }
}