package com.team01.backend.domain.post.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.post.entity.Post
import java.time.LocalDateTime

@JvmRecord
data class PostWriteResponse(
    val id: Long?,
    val title: String,
    val content: String,
    val boardId: Long?,
    val boardName: String,
    val categoryId: Long?,
    val categoryName: String,
    val authorId: Long?,
    val authorNickname: String,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime?,

    val postsCount: Long // 기존 응답에 없던 postsCount를 추가하여 평탄화
) {
    companion object {
        @JvmStatic
        fun of(post: Post, postsCount: Long): PostWriteResponse {
            return PostWriteResponse(
                id = post.getId(),
                title = post.getTitle(),
                content = post.getContent(),
                boardId = post.getBoard()?.getId(),
                boardName = post.getBoard()?.getName() ?: "",
                categoryId = post.getCategory()?.getId(),
                categoryName = post.getCategory()?.getName() ?: "",
                authorId = post.getAuthor()?.getId(),
                authorNickname = post.getAuthor()?.getNickname() ?: "",
                createdAt = post.getCreatedAt(),
                modifiedAt = post.getModifiedAt(),
                postsCount = postsCount
            )
        }
    }
}
