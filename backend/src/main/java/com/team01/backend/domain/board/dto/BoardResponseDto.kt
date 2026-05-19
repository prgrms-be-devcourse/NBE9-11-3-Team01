package com.team01.backend.domain.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.board.entity.Board

import java.time.LocalDateTime

data class BoardResponseDto(
    val id: Long,
    val boardName: String,
    val description: String?,
    val postCount: Long,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime,
) {
    companion object {
        fun of(board: Board, postCount: Long): BoardResponseDto = BoardResponseDto(
            id = board.id ?: throw IllegalStateException("Board id is null"),
            boardName = board.name,
            description = board.description,
            postCount = postCount,
            createdAt = board.createdAt ?: throw IllegalStateException("Board createdAt is null"),
            modifiedAt = board.modifiedAt ?: throw IllegalStateException("Board modifiedAt is null"),
        )

        fun from(board: Board, postCount: Long): BoardResponseDto = of(board, postCount)
    }
}
