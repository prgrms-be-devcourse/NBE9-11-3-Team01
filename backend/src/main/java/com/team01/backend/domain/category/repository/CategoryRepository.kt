package com.team01.backend.domain.category.repository

import com.team01.backend.domain.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun existsByBoardIdAndName(boardId: Long, name: String): Boolean
    @Query(
        """
        SELECT c FROM Category c
        JOIN Board b ON b.id = c.boardId
        WHERE c.boardId = :boardId
          AND b.deleted = false
        """
    )
    fun findByBoardIdAndBoardDeletedFalse(@Param("boardId") boardId: Long): List<Category>
}
