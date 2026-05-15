package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.dto.*
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

/**
 * [과제 제출용] 사용자 인증 및 권한 관리 서비스 (최종 통합본)
 * 조치 완료: F1(관리자 가입 로직 삭제, 로그 노출 차단), F2(비밀번호 정책 통일)
 * 보안 조치: 일반 가입 API를 통한 관리자 생성 경로를 완전히 차단했습니다.
 */
@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * 회원가입: 일반 가입 API를 통하는 모든 사용자는 Role.USER로 고정됩니다. (F1 대응)
     */
    fun signUp(request: SignUpRequest) {
        val safeEmail = request.email
        val safePassword = request.password
        val safeNickname = request.nickname

        if (userRepository.existsByEmail(safeEmail)) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
        }

        // 보안 지침에 따라 모든 일반 가입자는 Role.USER로 고정 (admin 필드 무시)
        val user = User(
            email = safeEmail,
            password = passwordEncoder.encode(safePassword) ?: throw IllegalStateException("비밀번호 인코딩에 실패했습니다."),
            nickname = safeNickname,
            role = Role.USER 
        )
        userRepository.save(user)
    }

    /**
     * 로그인: 토큰 세트 발급 및 DB 리프레시 토큰 각인
     */
    fun login(request: LoginRequest): TokenDto {
        val safeEmail = request.email
        val safePassword = request.password

        val user = userRepository.findByEmail(safeEmail)
            .orElseThrow { BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.") }

        if (user.role == Role.WITHDRAWN) throw IllegalArgumentException("탈퇴한 회원입니다.")
        
        if (!passwordEncoder.matches(safePassword, user.password)) {
            throw BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.")
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.email, user.role.name)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.email)

        // DB에 최신 리프레시 토큰 저장
        user.updateRefreshToken(refreshToken)
        
        return TokenDto(accessToken, refreshToken)
    }

    /**
     * [복구] 토큰 재발급 (Reissue): RTR(Refresh Token Rotation) 전략 기반
     * 리프레시 토큰을 검증하고 새로운 액세스 토큰을 발행합니다.
     */
    fun reissue(refreshToken: String): String {
        // 1. 토큰 자체 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw TokenReissueException("REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다. 다시 로그인하십시오.")
        }

        // 2. 토큰에서 이메일 추출 후 사용자 조회
        val email = jwtTokenProvider.getUserEmail(refreshToken)
        val user = userRepository.findByEmail(email)
            .orElseThrow { TokenReissueException("REFRESH_TOKEN_USER_NOT_FOUND", "인증 대상 사용자를 찾을 수 없습니다. 다시 로그인하십시오.") }

        // 3. DB에 저장된 토큰과 일치하는지 확인 (보안 강화)
        if (user.refreshToken != refreshToken) {
            throw TokenReissueException("REFRESH_TOKEN_MISMATCH", "유효하지 않은 인증 시도입니다. 다시 로그인하십시오.")
        }

        // 4. 새로운 액세스 토큰 생성 및 반환
        return jwtTokenProvider.createAccessToken(user.email, user.role.name)
    }

    /**
     * [추가] 비밀번호 재설정: 비밀번호를 분실한 사용자를 위한 구제 로직
     */
    @Transactional
    fun resetPassword(request: PasswordResetRequest) {
        val safeEmail = request.email
        val safeNewPassword = request.newPassword

        val user = userRepository.findByEmail(safeEmail)
            .orElseThrow { EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.") }
        
        // 새로운 비밀번호 암호화 후 업데이트
        user.updatePassword(passwordEncoder.encode(safeNewPassword) ?: throw IllegalStateException("비밀번호 인코딩에 실패했습니다."))
    }

    /**
     * 로그아웃: DB의 토큰 무효화
     */
    fun logout(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("사용자를 찾을 수 없습니다.") }
        user.updateRefreshToken(null)
    }

    /**
     * [복구] 아이디(이메일) 찾기
     */
    @Transactional(readOnly = true)
    fun findId(request: FindIdRequest): String {
        val safeNickname = request.nickname
        val user = userRepository.findByNicknameAndRoleNot(safeNickname, Role.WITHDRAWN)
            .orElseThrow { EntityNotFoundException("해당 닉네임으로 등록된 이메일이 없습니다.") }
            
        return user.email
    }

    /**
     * [복구] 회원 탈퇴: Soft Delete 처리
     */
    fun withdraw(email: String) {
        val user = userRepository.findByEmailAndRoleNot(email, Role.WITHDRAWN)
            .orElseThrow { EntityNotFoundException("활성 사용자를 찾을 수 없습니다.") }
        user.withdraw()
    }
}