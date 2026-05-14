package com.team01.backend.domain.board.dto

import com.team01.backend.domain.board.entity.Board
import java.time.LocalDateTime

data class BoardResponse(
    val id: Long,
    val boardName: String,
    val description: String,
    val postCount: Long,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {
    companion object {
        fun from(board: Board, postCount: Long): BoardResponse {
            return BoardResponse(
                board.id,
                board.name,
                board.description,
                postCount,
                board.getCreatedAt(),
                board.getModifiedAt()
            )
        }
    }
}
