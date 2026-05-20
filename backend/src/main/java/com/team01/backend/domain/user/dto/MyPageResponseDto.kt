package com.team01.backend.domain.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class MyPageResponseDto(
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val role: String,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime?
)
