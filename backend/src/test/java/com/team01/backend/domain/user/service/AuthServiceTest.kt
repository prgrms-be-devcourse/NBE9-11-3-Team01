package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.global.entity.BaseEntity
import com.team01.backend.global.security.JwtTokenProvider
import com.team01.backend.global.security.TokenReissueException
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder
    @Mock lateinit var jwtTokenProvider: JwtTokenProvider

    @InjectMocks lateinit var authService: AuthService

    // -------------------------------------------------------------------------
    // signUp
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("signUp - 정상 회원가입 시 저장 성공")
    fun t1() {
        // given
        whenever(userRepository.existsByEmail("user@test.com")).thenReturn(false)
        whenever(passwordEncoder.encode("password1234")).thenReturn("encodedPassword")

        // when
        authService.signUp("user@test.com", "password1234", "테스터")

        // then
        verify(userRepository).save(any())
    }

    @Test
    @DisplayName("signUp - 이메일 중복 시 IllegalArgumentException")
    fun t2() {
        // given
        whenever(userRepository.existsByEmail("user@test.com")).thenReturn(true)

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            authService.signUp("user@test.com", "password1234", "테스터")
        }
        verifyNoInteractions(passwordEncoder)
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login - 정상 로그인 시 AccessToken / RefreshToken 반환")
    fun t3() {
        // given
        val user = user(id = 100L, email = "user@test.com", password = "encodedPassword", role = Role.USER)
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)
        whenever(passwordEncoder.matches("password1234", "encodedPassword")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken("user@test.com", "USER")).thenReturn("accessToken")
        whenever(jwtTokenProvider.createRefreshToken("user@test.com")).thenReturn("refreshToken")

        // when
        val result = authService.login("user@test.com", "password1234")

        // then
        assertEquals("accessToken", result.accessToken)
        assertEquals("refreshToken", result.refreshToken)
    }

    @Test
    @DisplayName("login - 존재하지 않는 이메일이면 BadCredentialsException")
    fun t4() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(BadCredentialsException::class.java) {
            authService.login("none@test.com", "password1234")
        }
        verifyNoInteractions(passwordEncoder, jwtTokenProvider)
    }

    @Test
    @DisplayName("login - 비밀번호 불일치 시 BadCredentialsException")
    fun t5() {
        // given
        val user = user(id = 100L, email = "user@test.com", password = "encodedPassword")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)
        whenever(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false)

        // when & then
        assertThrows(BadCredentialsException::class.java) {
            authService.login("user@test.com", "wrongPassword")
        }
        verifyNoInteractions(jwtTokenProvider)
    }

    @Test
    @DisplayName("login - 탈퇴 회원 로그인 시 IllegalArgumentException")
    fun t6() {
        // given
        val withdrawnUser = user(id = 100L, email = "user@test.com", role = Role.WITHDRAWN)
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(withdrawnUser)

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            authService.login("user@test.com", "password1234")
        }
        verifyNoInteractions(passwordEncoder, jwtTokenProvider)
    }

    // -------------------------------------------------------------------------
    // reissue
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("reissue - 유효한 리프레시 토큰으로 새 AccessToken 재발급")
    fun t7() {
        // given
        val user = user(id = 100L, email = "user@test.com", role = Role.USER)
        user.updateRefreshToken("validRefreshToken")
        whenever(jwtTokenProvider.validateToken("validRefreshToken")).thenReturn(true)
        whenever(jwtTokenProvider.getUserEmail("validRefreshToken")).thenReturn("user@test.com")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)
        whenever(jwtTokenProvider.createAccessToken("user@test.com", "USER")).thenReturn("newAccessToken")

        // when
        val result = authService.reissue("validRefreshToken")

        // then
        assertEquals("newAccessToken", result)
        verify(jwtTokenProvider).createAccessToken("user@test.com", "USER")
    }

    @Test
    @DisplayName("reissue - 만료된 리프레시 토큰이면 TokenReissueException")
    fun t8() {
        // given
        whenever(jwtTokenProvider.validateToken("expiredToken")).thenReturn(false)

        // when & then
        assertThrows(TokenReissueException::class.java) {
            authService.reissue("expiredToken")
        }
        verifyNoInteractions(userRepository)
    }

    @Test
    @DisplayName("reissue - DB 토큰과 불일치 시 TokenReissueException")
    fun t9() {
        // given
        val user = user(id = 100L, email = "user@test.com")
        user.updateRefreshToken("storedToken")
        whenever(jwtTokenProvider.validateToken("requestToken")).thenReturn(true)
        whenever(jwtTokenProvider.getUserEmail("requestToken")).thenReturn("user@test.com")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when & then
        assertThrows(TokenReissueException::class.java) {
            authService.reissue("requestToken")
        }
    }

    // -------------------------------------------------------------------------
    // resetPassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resetPassword - 정상 비밀번호 재설정")
    fun t10() {
        // given
        val user = user(id = 100L, email = "user@test.com", password = "oldEncodedPassword")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)
        whenever(passwordEncoder.encode("newPassword1234")).thenReturn("newEncodedPassword")

        // when
        authService.resetPassword("user@test.com", "newPassword1234")

        // then
        assertEquals("newEncodedPassword", user.password)
    }

    @Test
    @DisplayName("resetPassword - 존재하지 않는 이메일이면 EntityNotFoundException")
    fun t11() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            authService.resetPassword("none@test.com", "newPassword1234")
        }
        verifyNoInteractions(passwordEncoder)
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("logout - 정상 로그아웃 시 리프레시 토큰 무효화")
    fun t12() {
        // given
        val user = user(id = 100L, email = "user@test.com")
        user.updateRefreshToken("refreshToken")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        authService.logout("user@test.com")

        // then
        assertNull(user.refreshToken)
    }

    @Test
    @DisplayName("logout - 존재하지 않는 이메일이면 EntityNotFoundException")
    fun t13() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            authService.logout("none@test.com")
        }
    }

    // -------------------------------------------------------------------------
    // findId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findId - 닉네임으로 이메일 조회 성공")
    fun t14() {
        // given
        val user = user(id = 100L, email = "user@test.com")
        whenever(userRepository.findByNicknameAndRoleNot("테스터", Role.WITHDRAWN)).thenReturn(user)

        // when
        val result = authService.findId("테스터")

        // then
        assertEquals("user@test.com", result)
    }

    @Test
    @DisplayName("findId - 존재하지 않는 닉네임이면 EntityNotFoundException")
    fun t15() {
        // given
        whenever(userRepository.findByNicknameAndRoleNot("없는닉네임", Role.WITHDRAWN)).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            authService.findId("없는닉네임")
        }
    }

    // -------------------------------------------------------------------------
    // withdraw
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("withdraw - 정상 탈퇴 시 Role WITHDRAWN으로 변경")
    fun t16() {
        // given
        val user = user(id = 100L, email = "user@test.com", role = Role.USER)
        whenever(userRepository.findByEmailAndRoleNot("user@test.com", Role.WITHDRAWN)).thenReturn(user)

        // when
        authService.withdraw("user@test.com")

        // then
        assertEquals(Role.WITHDRAWN, user.role)
    }

    @Test
    @DisplayName("withdraw - 이미 탈퇴한 회원이면 EntityNotFoundException")
    fun t17() {
        // given
        whenever(userRepository.findByEmailAndRoleNot("user@test.com", Role.WITHDRAWN)).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            authService.withdraw("user@test.com")
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private fun user(
        id: Long = 100L,
        email: String = "user@test.com",
        password: String = "encodedPassword",
        nickname: String = "테스터",
        role: Role = Role.USER,
    ): User = User(
        email = email,
        password = password,
        nickname = nickname,
        role = role,
    ).apply { setBaseFields(id = id) }

    private fun BaseEntity.setBaseFields(
        id: Long,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 5, 18, 12, 0),
        modifiedAt: LocalDateTime = createdAt,
    ) {
        setField("id", id)
        setField("createdAt", createdAt)
        setField("modifiedAt", modifiedAt)
    }

    private fun BaseEntity.setField(name: String, value: Any) {
        val field = BaseEntity::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }
}