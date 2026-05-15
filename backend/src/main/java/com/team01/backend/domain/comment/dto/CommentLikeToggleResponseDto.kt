package com.team01.backend.domain.comment.dto

class CommentLikeToggleResponseDto(
    val commentId: Long,
    val likeCount: Int,
    val liked: Boolean,
)
