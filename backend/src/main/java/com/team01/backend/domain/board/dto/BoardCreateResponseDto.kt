package com.team01.backend.domain.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.board.entity.Board
import java.time.LocalDateTime

data class BoardCreateResponseDto(
    val id: Long,
    val name: String,
    val description: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime
) {
    constructor(board: Board) : this(
        board.id,
        board.name,
        board.description,
        board.createdAt
    )
    companion object {
        fun from(board: Board): BoardCreateResponseDto =
            BoardCreateResponseDto(
                id = board.id,
                name = board.name,
                description = board.description,
                createdAt = board.createdAt,
            )
    }
}
