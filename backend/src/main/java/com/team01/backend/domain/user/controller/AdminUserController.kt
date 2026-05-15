package com.team01.backend.domain.user.controller

import com.team01.backend.domain.user.dto.UserResponseDto
import com.team01.backend.domain.user.service.AdminUserService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 회원 관리")
@RestController
class AdminUserController(private val adminUserService: AdminUserService) {

    @Operation(summary = "회원 목록 조회")
    @GetMapping("/admin/users")
    fun getAllUser(): ResponseEntity<ApiResponse<List<UserResponseDto>>> {
        val users = adminUserService.getAllUser()
        return ResponseEntity.ok(ApiResponse.ofSuccess(users))
    }
}
