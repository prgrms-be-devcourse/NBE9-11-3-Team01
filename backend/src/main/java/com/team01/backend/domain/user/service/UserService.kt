package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.dto.*
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
     * [복구] SseController.java 등 자바 레이어와의 호환성을 위한 사용자 ID 조회 로직
     */
    @Transactional(readOnly = true)
    fun findIdByUsername(username: String): Long {
        return userRepository.findByEmail(username)
            .map { user -> user.id ?: throw IllegalStateException("사용자 ID가 없습니다: $username") }
            .orElseThrow { EntityNotFoundException("사용자를 찾을 수 없습니다: $username") }
    }

    @Transactional(readOnly = true)
    fun getMyPage(email: String): MyPageResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("사용자 정보를 찾을 수 없습니다: $email") }

        return MyPageResponse(
            email = user.email,
            nickname = user.nickname,
            profileImage = user.profileImage,
            role = user.role.name,
            createdAt = user.createdAt
        )
    }

    /**
     * 사용자 정보 업데이트
     */
    fun updateUserInfo(email: String, request: UserUpdateInfoRequest) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("사용자 없음") }

        val finalPassword = request.newPassword
            ?.takeIf { newPassword -> newPassword.isNotBlank() }
            ?.let { newPassword -> passwordEncoder.encode(newPassword) }
            ?: user.password

        user.updateInfo(request.nickname, finalPassword)
    }

    /**
     * 프로필 이미지 업데이트
     */
    fun updateProfileImage(email: String, request: UserProfileImageRequest) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("사용자 없음") }
        
        user.updateProfileImage(request.profileImage ?: user.profileImage)
    }
}