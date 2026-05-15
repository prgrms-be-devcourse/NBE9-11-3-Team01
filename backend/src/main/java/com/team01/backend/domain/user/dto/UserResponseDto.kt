package com.team01.backend.domain.user.dto

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User

data class UserResponseDto(
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val role: Role
) {
    constructor(user: User) : this(
        user.email,
        user.nickname,
        user.profileImage,
        user.role
    )
}
