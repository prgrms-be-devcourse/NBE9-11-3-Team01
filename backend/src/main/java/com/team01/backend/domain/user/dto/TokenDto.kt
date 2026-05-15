package com.team01.backend.domain.user.dto

data class TokenDto(
    val accessToken: String,
    val refreshToken: String
)
