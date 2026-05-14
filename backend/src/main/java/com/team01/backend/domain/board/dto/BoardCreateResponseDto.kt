package com.team01.backend.domain.board.dto

import com.team01.backend.domain.board.entity.Board
import java.time.LocalDateTime

data class BoardCreateResponseDto(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime
) {
    constructor(board: Board) : this(
        board.id,
        board.name,
        board.description,
        board.createdAt
    )
}
