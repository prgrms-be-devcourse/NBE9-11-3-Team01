package com.team01.backend.domain.board.repository;

import com.team01.backend.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

interface BoardRepository : JpaRepository<Board,Long> {
    fun findByIdAndIsDeletedFalse(id: Long): Board?
    fun findAllByIsDeletedFalse(): List<Board>
    fun findAllByIsDeletedTrue(): List<Board>
    fun existsByNameAndIsDeletedFalse(name: String): Boolean
}
