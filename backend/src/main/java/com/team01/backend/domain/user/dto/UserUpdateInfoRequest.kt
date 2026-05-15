package com.team01.backend.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * [과제] 사용자 정보 수정을 위한 요청 객체입니다.
 * 이미지 경로 검증을 위해 정규표현식(Regex)을 적용했습니다.
 */
data class UserUpdateInfoRequest(
    @field:NotBlank(message = "닉네임은 필수입니다.")
    val nickname: String,

    // 정규식 설명: /images/profile/ 경로로 시작하며 허용된 확장자만 허용함 (XSS 방어)
    @field:Pattern(
        regexp = "^/static/images/.*$",
        message = "프로필 이미지는 /static/images/ 경로로 시작해야 합니다."
    )
    val profileImage: String? = null,

    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    val newPassword: String? = null
)