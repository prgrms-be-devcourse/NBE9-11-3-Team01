package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.global.security.JwtTokenProvider
import com.team01.backend.global.security.TokenReissueException
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AuthToken(
    val accessToken: String,
    val refreshToken: String
)

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * 회원 가입을 처리합니다.
     * 동일한 이메일이 존재하면 예외를 발생시킵니다.
     */
    fun signUp(email: String, password: String, nickname: String) {
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
        }

        val user = User(
            email = email,
            password = passwordEncoder.encode(password) ?: throw IllegalStateException("비밀번호 인코딩에 실패했습니다."),
            nickname = nickname,
            role = Role.USER 
        )
        userRepository.save(user)
    }

    /**
     * 로그인을 처리하고 새로운 AccessToken과 RefreshToken을 반환합니다.
     * 탈퇴한 회원이거나 비밀번호가 불일치하면 예외를 던집니다.
     */
    fun login(email: String, password: String): AuthToken {
        // Optional 제거 후 코틀린의 엘비스 연산자로 흐름을 완벽히 통제하네.
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.")

        if (user.role == Role.WITHDRAWN) throw IllegalArgumentException("탈퇴한 회원입니다.")
        
        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.")
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.email, user.role.name)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.email)

        // 영속성 엔티티의 리프레시 토큰 수정 (더티 체킹 발생)
        user.updateRefreshToken(refreshToken)

        return AuthToken(accessToken, refreshToken)
    }

    /**
     * 전달받은 RefreshToken의 유효성을 검증하여 새로운 AccessToken을 재발급합니다.
     * 토큰 만료, 불일치 등의 예외 상황을 세분화하여 처리합니다.
     */
    fun reissue(refreshToken: String): String {
        // 1. 리프레시 토큰 자체의 기본 만료 및 위변조 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw TokenReissueException("REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다. 다시 로그인하십시오.")
        }

        // 2. 토큰 내부 Payload에서 주체(이메일) 추출
        val email = jwtTokenProvider.getUserEmail(refreshToken)
        
        // 3. 추출한 이메일 기반으로 실제 유저 데이터 획득 (엘비스 연산자 적용)
        val user = userRepository.findByEmail(email)
            ?: throw TokenReissueException("REFRESH_TOKEN_USER_NOT_FOUND", "인증 대상 사용자를 찾을 수 없습니다. 다시 로그인하십시오.")

        // 4. 데이터베이스에 저장된 토큰 값과 클라이언트가 보낸 토큰 값 대조
        if (user.refreshToken != refreshToken) {
            throw TokenReissueException("REFRESH_TOKEN_MISMATCH", "유효하지 않은 인증 시도입니다. 다시 로그인하십시오.")
        }

        // 5. 검증 완료 시 새로운 AccessToken만 단독 발행하여 전송
        return jwtTokenProvider.createAccessToken(user.email, user.role.name)
    }

    /**
     * 사용자의 비밀번호를 강제로 재설정합니다.
     * 이메일 인증 코드가 성공적으로 확인된 이후에 호출되어야 안전합니다.
     */
    @Transactional
    fun resetPassword(email: String, newPassword: String) {
        val user = userRepository.findByEmail(email)
            ?: throw EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.")
        
        user.updatePassword(passwordEncoder.encode(newPassword) ?: throw IllegalStateException("비밀번호 인코딩에 실패했습니다."))
    }

    /**
     * 사용자의 로그아웃을 처리합니다.
     * DB의 리프레시 토큰을 무효화하여 토큰 도용 위험을 차단합니다.
     */
    fun logout(email: String) {
        val user = userRepository.findByEmail(email)
            ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다.")
        user.updateRefreshToken(null)
    }

    /**
     * 닉네임을 통해 가입된 사용자의 이메일 아이디를 추적합니다.
     * 탈퇴 회원 조건은 검색 대상에서 엄격히 배제합니다.
     */
    @Transactional(readOnly = true)
    fun findId(nickname: String): String {
        val user = userRepository.findByNicknameAndRoleNot(nickname, Role.WITHDRAWN)
            ?: throw EntityNotFoundException("해당 닉네임으로 등록된 이메일이 없습니다.")
            
        return user.email
    }

    /**
     * 활성화된 회원을 소프트 딜리트(Soft Delete) 방식으로 탈퇴시킵니다.
     * 데이터 정밀 분석 보존을 위해 상태값만 WITHDRAWN으로 변환합니다.
     */
    fun withdraw(email: String) {
        val user = userRepository.findByEmailAndRoleNot(email, Role.WITHDRAWN)
            ?: throw EntityNotFoundException("활성 사용자를 찾을 수 없습니다.")
        user.withdraw()
    }
}