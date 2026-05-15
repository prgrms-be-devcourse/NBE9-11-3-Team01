package com.team01.backend.domain.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 *  회원가입 시 클라이언트로부터 전달받는 요청 데이터 객체(DTO)입니다.
 * 롬복(Lombok) 대신 코틀린의 data class를 사용하여 가독성을 높였습니다.
 */
data class SignUpRequest(
    
    // 이메일 형식 검증: 필수 입력값이며 이메일 표준 형식을 준수해야 합니다.
    @field:NotBlank(message = "이메일은 필수 입력 사항입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    // 비밀번호 검증: 보안을 위해 최소 8자 이상으로 설정을 강제합니다.
    @field:NotBlank(message = "비밀번호는 필수 입력 사항입니다.")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    val password: String,

    // 사용자 닉네임: 중복되지 않는 고유한 별명을 입력받습니다.
    @field:NotBlank(message = "닉네임은 필수 입력 사항입니다.")
    val nickname: String,

    // 프로필 이미지 주소: 선택 사항이며, 입력하지 않을 경우 기본 프로필로 처리됩니다.
    val profileImage: String? = null

    // 보안 강화: 관리자 가입 토큰 및 권한 요청 필드는 보안상 위험하여 삭제하였습니다.
)