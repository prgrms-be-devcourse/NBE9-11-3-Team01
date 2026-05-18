package com.team01.backend.domain.board.repository

import com.team01.backend.domain.board.entity.Board
import org.springframework.data.jpa.repository.JpaRepository

interface BoardRepository : JpaRepository<Board,Long> {
    fun findByIdAndDeletedFalse(id: Long): Board?
    fun findAllByDeletedFalse(): List<Board>
    fun findAllByDeletedTrue(): List<Board>
    fun existsByNameAndDeletedFalse(name: String): Boolean
    fun existsByIdAndDeletedFalse(id: Long): Boolean
}
