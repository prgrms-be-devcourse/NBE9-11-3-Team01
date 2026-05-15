package com.team01.backend.domain.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.board.entity.Board
import java.time.LocalDateTime

data class BoardResponse(
    val id: Long,
    val boardName: String,
    val description: String,
    val postCount: Long,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime
) {
    companion object {
        fun from(board: Board, postCount: Long): BoardResponse {
            return BoardResponse(
                board.id,
                board.name,
                board.description,
                postCount,
                board.createdAt,
                board.modifiedAt
            )
        }
    }
}
