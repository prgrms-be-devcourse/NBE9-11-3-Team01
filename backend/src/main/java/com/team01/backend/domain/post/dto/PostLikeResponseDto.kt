package com.team01.backend.domain.post.dto


data class PostLikeResponseDto(
    val liked: Boolean,
    val likeCount: Int
)
