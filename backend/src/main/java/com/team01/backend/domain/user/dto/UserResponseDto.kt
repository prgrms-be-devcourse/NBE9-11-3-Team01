package com.team01.backend.domain.user.dto

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User

data class UserResponseDto(
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val role: Role
) {
    companion object {
        fun from(user: User): UserResponseDto =
            UserResponseDto(
                email = user.email,
                nickname = user.nickname,
                profileImage = user.profileImage,
                role = user.role
            )
    }
}
