package com.team01.backend.domain.post.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.post.entity.Post

import java.time.LocalDateTime

data class PostResponseDto(
    val id: Long,
    val title: String,
    val author: String,
    val profileImage: String?,
    val categoryId: Long,
    val categoryName: String,
    val likeCount: Int,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime,
) {
    companion object {
        fun of(post: Post): PostResponseDto = PostResponseDto(
            id = post.id ?: throw IllegalStateException("Post id is null"),
            title = post.title,
            author = post.author.nickname,
            profileImage = post.author.profileImage,
            categoryId = post.category.id ?: throw IllegalStateException("Category id is null"),
            categoryName = post.category.name,
            likeCount = post.likeCount,
            createdAt = post.createdAt ?: throw IllegalStateException("Post createdAt is null"),
            modifiedAt = post.modifiedAt ?: throw IllegalStateException("Post modifiedAt is null"),
        )

        fun from(post: Post): PostResponseDto = of(post)
    }
}