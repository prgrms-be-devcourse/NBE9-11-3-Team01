package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.dto.MyPageResponseDto
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * 스프링 시큐리티 내부 컨텍스트 연계를 위해 이메일 기반으로 유저의 고유 DB PK ID를 조회합니다.
     * 기존 자바 방식의 Optional.map 체인을 코틀린의 ?.let 문법과 엘비스 연산자로 최적화했네.
     */
    @Transactional(readOnly = true)
    fun findIdByUsername(username: String): Long {
        val user = userRepository.findByEmail(username)
            ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다: $username")

        return user.id
    }

    /**
     * 마이페이지 출력을 위한 전용 데이터 구조를 생성하여 반환합니다.
     * 패스워드 등 민감 정보는 철저히 마스킹되어 포함되지 않습니다.
     */
    @Transactional(readOnly = true)
    fun getMyPage(email: String): MyPageResponseDto {
        val user = userRepository.findByEmail(email)
            ?: throw EntityNotFoundException("사용자 정보를 찾을 수 없습니다: $email")

        return MyPageResponseDto(
            email = user.email,
            nickname = user.nickname,
            profileImage = user.profileImage,
            role = user.role.name,
            createdAt = user.createdAt
        )
    }

    /**
     * 회원의 닉네임과 비밀번호 자산을 안전하게 갱신합니다.
     * 새 패스워드가 빈 값(blank)일 경우 기존 자산을 원본 유지하도록 방어적으로 설계되었습니다.
     */
    fun updateUserInfo(email: String, nickname: String, newPassword: String?) {
        val user = userRepository.findByEmail(email)
            ?: throw EntityNotFoundException("사용자 없음")

        // 공백 검증 및 안전한 대입 체인 형성
        val finalPassword = newPassword
            ?.takeIf { password -> password.isNotBlank() }
            ?.let { password -> passwordEncoder.encode(password) }
            ?: user.password

        user.updateInfo(nickname, finalPassword)
    }

    /**
     * 사용자의 프로필 이미지 경로 자산을 교체합니다.
     * 입력 경로 값이 널(null)일 경우 기본값을 유지하도록 제어합니다.
     */
    fun updateProfileImage(email: String, profileImage: String?) {
        val user = userRepository.findByEmail(email)
            ?: throw EntityNotFoundException("사용자 없음")

        user.updateProfileImage(profileImage ?: user.profileImage)
    }
}