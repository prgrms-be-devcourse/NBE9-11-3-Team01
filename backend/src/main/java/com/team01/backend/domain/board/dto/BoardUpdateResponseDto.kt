package com.team01.backend.domain.board.dto

import com.team01.backend.domain.board.entity.Board
import java.time.LocalDateTime

data class BoardUpdateResponseDto(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {
    constructor(board: Board) : this(
        board.id,
        board.name,
        board.description,
        board.createdAt,
        board.modifiedAt
    )
}
