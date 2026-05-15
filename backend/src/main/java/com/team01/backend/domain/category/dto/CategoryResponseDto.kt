package com.team01.backend.domain.category.dto

import com.team01.backend.domain.category.entity.Category

data class CategoryResponseDto(
    val id: Long,
    val boardId: Long,
    val name: String
) {
    constructor(category: Category) : this(
        category.id,
        category.boardId,
        category.name
    )
}
