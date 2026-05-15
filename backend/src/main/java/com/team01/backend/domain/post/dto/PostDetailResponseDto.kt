package com.team01.backend.domain.post.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.category.entity.Category
import com.team01.backend.domain.comment.dto.CommentReadResponseDto
import com.team01.backend.domain.post.entity.Post

import java.time.LocalDateTime

data class PostDetailResponseDto(
        val id: Long,
        val boardId: Long,
        val boardName: String,
        val categoryId: Long,
        val categoryName: String,
        val title: String,
        val content: String,
        val author: String,
        val profileImage: String?,
        val likeCount: Int,
        @get:JsonProperty("isLiked")
        val liked: Boolean,
        @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        val createdAt: LocalDateTime,
        @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        val modifiedAt: LocalDateTime,
        val comments: List<CommentReadResponseDto>,
        @get:JsonProperty("isOwner")
        val owner: Boolean,
) {
        companion object {
                @JvmStatic
                fun of(
                        post: Post,
                        board: Board,
                        category: Category,
                        comments: List<CommentReadResponseDto>,
                        isOwner: Boolean,
                        isLiked: Boolean,
                ): PostDetailResponseDto = PostDetailResponseDto(
                        id = post.id ?: throw IllegalStateException("Post id is null"),
                        boardId = board.id ?: throw IllegalStateException("Board id is null"),
                        boardName = board.name,
                        categoryId = category.id ?: throw IllegalStateException("Category id is null"),
                        categoryName = category.name,
                        title = post.title,
                        content = post.content,
                        author = post.author.nickname,
                        profileImage = post.author.profileImage,
                        likeCount = post.likeCount,
                        liked = isLiked,
                        createdAt = post.createdAt,
                        modifiedAt = post.modifiedAt,
                        comments = comments,
                        owner = isOwner,
                )
        }
}
