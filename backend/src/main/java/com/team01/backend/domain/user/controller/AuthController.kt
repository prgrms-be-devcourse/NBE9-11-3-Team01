package com.team01.backend.domain.user.controller

import com.team01.backend.domain.user.dto.*
import com.team01.backend.domain.user.service.AuthService
import com.team01.backend.domain.user.service.MailService
import com.team01.backend.global.response.ApiResponse
import com.team01.backend.global.security.TokenReissueException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "인증", description = "로그인, 회원가입, 메일 인증 및 토큰 관리 API")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val mailService: MailService, // [복구 완료] 이메일 방어선 유지
    @Value("\${custom.cookie.secure:true}") private val isSecure: Boolean
) {

    data class SignUpRequest(
        @field:NotBlank(message = "이메일은 필수 입력 사항입니다.")
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        val email: String,

        @field:NotBlank(message = "비밀번호는 필수 입력 사항입니다.")
        @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        val password: String,

        @field:NotBlank(message = "닉네임은 필수 입력 사항입니다.")
        val nickname: String,

        @field:Pattern(
            regexp = "^/static/images/.*$",
            message = "프로필 이미지는 /static/images/ 경로로 시작해야 합니다."
        )
        val profileImage: String? = null
    )

    data class LoginRequest(
        @field:NotBlank @field:Email val email: String,
        @field:NotBlank @field:Size(min = 8) val password: String
    )

    data class FindIdRequest(
        @field:NotBlank val nickname: String
    )

    data class PasswordResetRequest(
        @field:NotBlank @field:Email val email: String,
        @field:NotBlank val verificationCode: String,
        @field:NotBlank @field:Size(min = 8) val newPassword: String
    )

    data class EmailSendRequest(val email: String)

    data class EmailVerifyRequest(val email: String, val code: String)

    // --- 공개 API 영역 ---

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<ApiResponse<Void>> {
        authService.signUp(
            email = request.email,
            password = request.password,
            nickname = request.nickname
        )
        // [규격 준수] Java ApiResponse의 ofSuccessWithoutBody 사용
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Void>> {
        val token = authService.login(request.email, request.password)
        setAuthCookies(response, token.accessToken, token.refreshToken)
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    @Operation(summary = "토큰 재발급", description = "만료된 액세스 토큰을 갱신합니다.")
    @PostMapping("/reissue", "/refresh")
    fun reissue(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Void>> {
        if (refreshToken.isNullOrBlank()) {
            throw TokenReissueException("REFRESH_TOKEN_MISSING", "인증 정보가 누락되었습니다. 다시 로그인하십시오.")
        }
        val newAccessToken = authService.reissue(refreshToken)
        val accessCookie = createCookie("accessToken", newAccessToken, 900)
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString())
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    @Operation(summary = "아이디 찾기")
    @PostMapping("/find-id")
    fun findId(@Valid @RequestBody request: FindIdRequest): ResponseEntity<ApiResponse<String>> {
        val email = authService.findId(request.nickname)
        return ResponseEntity.ok(ApiResponse.ofSuccess(email))
    }

    @Operation(summary = "비밀번호 재설정", description = "비밀번호를 잊어버린 경우 새로운 비밀번호로 설정합니다.")
    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<ApiResponse<Void>> {
        if (!mailService.verifyCode(request.email, request.verificationCode)) {
            throw IllegalArgumentException("인증 코드가 일치하지 않습니다.")
        }
        authService.resetPassword(request.email, request.newPassword)
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    // --- 이메일 인증 API (복구 및 장착 완료) ---

    @Operation(summary = "이메일 인증 코드 발송", description = "입력한 이메일로 6자리 인증 코드를 발송합니다.")
    @PostMapping("/email/send")
    fun sendEmailCode(@RequestBody request: EmailSendRequest): ResponseEntity<ApiResponse<Void>> {
        mailService.sendVerificationCode(request.email)
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    @Operation(summary = "이메일 인증 코드 확인")
    @PostMapping("/email/verify")
    fun verifyEmailCode(@RequestBody request: EmailVerifyRequest): ResponseEntity<ApiResponse<Void>> {
        val isVerified = mailService.verifyCode(request.email, request.code)
        if (!isVerified) throw IllegalArgumentException("인증 코드가 일치하지 않습니다.")
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    // --- 인증 필요 API 영역 ---

    @Operation(summary = "로그아웃")
    @SecurityRequirement(name = "cookieAuth")
    @PostMapping("/logout")
    fun logout(
        authentication: Authentication?, // [유지] Null 허용
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Void>> {
        if (authentication == null) throw IllegalArgumentException("인증 정보가 없습니다.")
        authService.logout(authentication.name)
        clearAuthCookies(response)
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    @Operation(summary = "회원 탈퇴")
    @SecurityRequirement(name = "cookieAuth")
    @DeleteMapping("/withdraw")
    fun withdraw(
        authentication: Authentication?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Void>> {
        if (authentication == null) throw IllegalArgumentException("인증 정보가 없습니다.")
        authService.withdraw(authentication.name)
        clearAuthCookies(response)
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    // --- 유틸리티 메서드 ---

    private fun setAuthCookies(response: HttpServletResponse, accessToken: String, refreshToken: String) {
        val accessCookie = createCookie("accessToken", accessToken, 900)
        val refreshCookie = createCookie("refreshToken", refreshToken, 604800)
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString())
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())
    }

    private fun clearAuthCookies(response: HttpServletResponse) {
        val accessCookie = createCookie("accessToken", "", 0)
        val refreshCookie = createCookie("refreshToken", "", 0)
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString())
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())
    }

    private fun createCookie(name: String, value: String, maxAge: Int): ResponseCookie {
        return ResponseCookie.from(name, value)
            .httpOnly(true).secure(isSecure).path("/").maxAge(maxAge.toLong()).sameSite("Strict").build()
    }
}