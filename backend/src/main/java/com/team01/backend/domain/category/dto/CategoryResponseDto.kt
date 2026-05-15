package com.team01.backend.domain.category.dto

import com.team01.backend.domain.category.entity.Category

data class CategoryResponseDto(
    val id: Long,
    val boardId: Long,
    val name: String
) {
    companion object {
        fun from(category: Category): CategoryResponseDto =
            CategoryResponseDto(
                id = category.id,
                boardId = category.boardId,
                name = category.name,
            )
    }
}
