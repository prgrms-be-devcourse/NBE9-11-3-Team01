package com.team01.backend.domain.comment.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.comment.entity.Comment
import java.time.LocalDateTime

data class CommentResponseDto(
    val id: Long,
    val content: String,
    val author: String,
    val likeCount: Int,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime,
) {
    companion object {
        // 컨벤션: 정적 팩토리 메서드 of 사용
        fun of(comment: Comment): CommentResponseDto =
            CommentResponseDto(
                id = comment.id,
                content = comment.content,
                author = comment.user.nickname,
                likeCount = comment.likeCount,
                createdAt = comment.createdAt,
                modifiedAt = comment.modifiedAt,
            )

        fun from(comment: Comment): CommentResponseDto = of(comment)
    }
}
