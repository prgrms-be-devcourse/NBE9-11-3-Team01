package com.team01.backend.domain.user.controller

import com.team01.backend.domain.user.dto.MyPageResponseDto
import com.team01.backend.domain.user.service.UserService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "사용자", description = "사용자 정보 관리 API")
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
class UserController(private val userService: UserService) {

    data class UserUpdateInfoRequest(
        @field:NotBlank(message = "닉네임은 필수입니다.")
        val nickname: String,

        @field:Pattern(
            regexp = "^/static/images/.*$",
            message = "프로필 이미지는 /static/images/ 경로로 시작해야 합니다."
        )
        val profileImage: String? = null,

        @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        val newPassword: String? = null
    )

    data class UserProfileImageRequest(
        @field:Pattern(
            regexp = "^/static/images/.*$",
            message = "프로필 이미지는 /static/images/ 경로로 시작해야 합니다."
        )
        val profileImage: String?
    )

    @Operation(summary = "마이페이지 조회")
    @GetMapping("/me")
    fun getMyPage(authentication: Authentication): ResponseEntity<ApiResponse<MyPageResponseDto>> {
        val response = userService.getMyPage(authentication.name)
        return ResponseEntity.ok(ApiResponse.ofSuccess(response))
    }

    @Operation(summary = "사용자 정보 갱신")
    @PutMapping("/me/info")
    fun updateUserInfo(
        @Valid @RequestBody request: UserUpdateInfoRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Void>> {
        userService.updateUserInfo(
            email = authentication.name,
            nickname = request.nickname,
            newPassword = request.newPassword
        )
        // [수정] global 규격에 맞춰 메시지 제거 및 ofSuccessWithoutBody 사용
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    @Operation(summary = "프로필 이미지 갱신")
    @PutMapping("/me/profile-image")
    fun updateProfileImage(
        @Valid @RequestBody request: UserProfileImageRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Void>> {
        userService.updateProfileImage(authentication.name, request.profileImage)
        // [수정] 규격 준수
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }
}