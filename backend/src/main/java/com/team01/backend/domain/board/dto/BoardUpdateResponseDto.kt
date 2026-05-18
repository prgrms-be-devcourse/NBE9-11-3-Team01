package com.team01.backend.domain.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.board.entity.Board
import java.time.LocalDateTime

data class BoardUpdateResponseDto(
    val id: Long,
    val name: String,
    val description: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime
) {
    constructor(board: Board) : this(
        board.id,
        board.name,
        board.description,
        board.createdAt,
        board.modifiedAt
    )
    companion object {
        @JvmStatic
        fun from(board: Board): BoardUpdateResponseDto =
            BoardUpdateResponseDto(
                id = board.id,
                name = board.name,
                description = board.description,
                createdAt = board.createdAt,
                modifiedAt = board.modifiedAt,
            )
    }
}
