package com.team01.backend.domain.user.dto

import jakarta.validation.constraints.Pattern

data class UserProfileImageRequest(
    @field:Pattern(
        regexp = "^/static/images/.*$",
        message = "프로필 이미지는 /static/images/ 경로로 시작해야 합니다."
    )
    val profileImage: String?
)
