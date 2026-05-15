package com.team01.backend.domain.post.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.post.entity.Post
import java.time.LocalDateTime

data class PostWriteResponse(
    val id: Long,
    val title: String,
    val content: String,
    val boardId: Long,
    val boardName: String,
    val categoryId: Long,
    val categoryName: String,
    val authorId: Long,
    val authorNickname: String,

    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime,

    val postsCount: Long
) {
    constructor(post: Post, postsCount: Long) : this(
        id = post.id ?: throw IllegalStateException("Post id is null"),
        title = post.title,
        content = post.content,

        boardId = post.board.id ?: throw IllegalStateException("Board id is null"),
        boardName = post.board.name,

        categoryId = post.category.id ?: throw IllegalStateException("Category id is null"),
        categoryName = post.category.name,

        authorId = post.author.id ?: throw IllegalStateException("User id is null"),
        authorNickname = post.author.nickname,

        createdAt = post.createdAt ?: throw IllegalStateException("createdAt is null"),
        modifiedAt = post.modifiedAt ?: throw IllegalStateException("modifiedAt is null"),
        postsCount = postsCount
    )

    companion object {
        @JvmStatic
        fun of(post: Post, postsCount: Long): PostWriteResponse = PostWriteResponse(post, postsCount)
    }
}