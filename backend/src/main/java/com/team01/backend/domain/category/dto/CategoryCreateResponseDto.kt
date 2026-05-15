package com.team01.backend.domain.category.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.category.entity.Category
import java.time.LocalDateTime

data class CategoryCreateResponseDto(
    val id: Long,
    val boardId: Long,
    val name: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime
) {
    companion object {
        fun from(category: Category): CategoryCreateResponseDto =
            CategoryCreateResponseDto(
                id = category.id,
                boardId = category.boardId,
                name = category.name,
                createdAt = category.createdAt,
                modifiedAt = category.modifiedAt
            )
    }
}
