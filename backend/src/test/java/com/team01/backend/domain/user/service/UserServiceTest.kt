package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks lateinit var userService: UserService

    // -------------------------------------------------------------------------
    // findIdByUsername
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findIdByUsername - 이메일로 유저 ID 조회 성공")
    fun t1() {
        // given
        val user = user(id = 100L, email = "user@test.com")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        val result = userService.findIdByUsername("user@test.com")

        // then
        assertEquals(100L, result)
    }

    @Test
    @DisplayName("findIdByUsername - 존재하지 않는 이메일이면 EntityNotFoundException")
    fun t2() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            userService.findIdByUsername("none@test.com")
        }
    }

    // -------------------------------------------------------------------------
    // getMyPage
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getMyPage - 정상 마이페이지 조회")
    fun t3() {
        // given
        val user = user(id = 100L, email = "user@test.com", nickname = "테스터", role = Role.USER)
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        val result = userService.getMyPage("user@test.com")

        // then
        assertEquals("user@test.com", result.email)
        assertEquals("테스터", result.nickname)
        assertEquals(Role.USER.name, result.role)
    }

    @Test
    @DisplayName("getMyPage - 존재하지 않는 이메일이면 EntityNotFoundException")
    fun t4() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            userService.getMyPage("none@test.com")
        }
    }

    // -------------------------------------------------------------------------
    // updateUserInfo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateUserInfo - 닉네임 + 비밀번호 정상 변경")
    fun t5() {
        // given
        val user = user(id = 100L, email = "user@test.com", password = "oldEncoded")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)
        whenever(passwordEncoder.encode("newPassword1234")).thenReturn("newEncoded")

        // when
        userService.updateUserInfo("user@test.com", "새닉네임", "newPassword1234")

        // then
        assertEquals("새닉네임", user.nickname)
        assertEquals("newEncoded", user.password)
    }

    @Test
    @DisplayName("updateUserInfo - 비밀번호 null 입력 시 기존 비밀번호 유지")
    fun t6() {
        // given
        val user = user(id = 100L, email = "user@test.com", password = "oldEncoded")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        userService.updateUserInfo("user@test.com", "새닉네임", null)

        // then
        assertEquals("새닉네임", user.nickname)
        assertEquals("oldEncoded", user.password)
    }

    @Test
    @DisplayName("updateUserInfo - 공백 비밀번호 입력 시 기존 비밀번호 유지")
    fun t7() {
        // given
        val user = user(id = 100L, email = "user@test.com", password = "oldEncoded")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        userService.updateUserInfo("user@test.com", "새닉네임", "   ")

        // then
        assertEquals("oldEncoded", user.password)
    }

    @Test
    @DisplayName("updateUserInfo - 존재하지 않는 이메일이면 EntityNotFoundException")
    fun t8() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            userService.updateUserInfo("none@test.com", "새닉네임", null)
        }
    }

    // -------------------------------------------------------------------------
    // updateProfileImage
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateProfileImage - 정상 프로필 이미지 변경")
    fun t9() {
        // given
        val user = user(id = 100L, email = "user@test.com")
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        userService.updateProfileImage("user@test.com", "https://example.com/new.jpg")

        // then
        assertEquals("https://example.com/new.jpg", user.profileImage)
    }

    @Test
    @DisplayName("updateProfileImage - null 입력 시 기존 이미지 유지")
    fun t10() {
        // given
        val user = user(id = 100L, email = "user@test.com")
        val originalImage = user.profileImage
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(user)

        // when
        userService.updateProfileImage("user@test.com", null)

        // then
        assertEquals(originalImage, user.profileImage)
    }

    @Test
    @DisplayName("updateProfileImage - 존재하지 않는 이메일이면 EntityNotFoundException")
    fun t11() {
        // given
        whenever(userRepository.findByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            userService.updateProfileImage("none@test.com", "https://example.com/new.jpg")
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