package com.team01.backend.domain.category.repository

import com.team01.backend.domain.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun existsByBoardIdAndName(boardId: Long, name: String): Boolean
    fun findByBoardId(boardId: Long): List<Category>
}
