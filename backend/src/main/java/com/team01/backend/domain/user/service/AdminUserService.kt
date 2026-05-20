package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.dto.UserResponseDto
import com.team01.backend.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AdminUserService(private val userRepository: UserRepository) {
    fun getAllUser(): List<UserResponseDto> {
        return userRepository.findAll().map { UserResponseDto(it) }
    }
}
