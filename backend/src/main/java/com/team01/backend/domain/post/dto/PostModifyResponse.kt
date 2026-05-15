package com.team01.backend.domain.post.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.post.entity.Post
import java.time.LocalDateTime


data class PostModifyResponse(
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
    val modifiedAt: LocalDateTime?
) {
        // 컨벤션 3번: 정적 팩토리 메서드 of 사용
        companion object {
            @JvmStatic
            fun of(post: Post): PostModifyResponse {
                return PostModifyResponse(
                    // [수정] .id 대신 .getId() 메서드를 직접 호출
                    id = post.getId(),
                    title = post.title,
                    content = post.content,

                    // 연관 엔티티들도 게터로 접근
                    boardId = post.board?.getId(),
                    boardName = post.board?.name ?: "",

                    categoryId = post.category?.getId(),
                    categoryName = post.category?.name ?: "",

                    authorId = post.author?.getId(),
                    authorNickname = post.author?.nickname ?: "",

                    // BaseEntity 필드들도 게터로 접근
                    createdAt = post.getCreatedAt(),
                    modifiedAt = post.getModifiedAt()
                )
            }
        }
    }

