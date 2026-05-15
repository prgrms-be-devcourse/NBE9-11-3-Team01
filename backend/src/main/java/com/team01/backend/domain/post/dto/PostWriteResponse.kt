package com.team01.backend.domain.post.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.post.entity.Post
import java.time.LocalDateTime


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

    val postsCount: Long
) {
    companion object {
        @JvmStatic
        fun of(post: Post, postsCount: Long): PostWriteResponse {
            return PostWriteResponse(
                id = post.getId(),
                title = post.getTitle(),
                content = post.getContent(),

                // [수정] Board가 코틀린으로 전환됨에 따라 프로퍼티 접근
                boardId = post.getBoard()?.id,
                boardName = post.getBoard()?.name ?: "",

                // Category나 Author도 전환되면 프로퍼티로 변경 필요
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