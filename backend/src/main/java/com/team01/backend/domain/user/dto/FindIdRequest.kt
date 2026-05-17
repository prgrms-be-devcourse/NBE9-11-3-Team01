package com.team01.backend.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class FindIdRequest(
    @field:NotBlank val nickname: String
)
