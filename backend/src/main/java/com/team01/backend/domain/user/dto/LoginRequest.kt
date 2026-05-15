package com.team01.backend.domain.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 4) val password: String
)
