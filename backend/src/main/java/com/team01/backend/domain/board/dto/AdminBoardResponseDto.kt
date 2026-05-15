package com.team01.backend.domain.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.team01.backend.domain.board.entity.Board

import java.time.LocalDateTime

data class AdminBoardResponseDto(
    val id: Long,
    val boardName: String,
    val description: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime,
    val isDeleted: Boolean
) {
    constructor (board: Board):this(
        board.id,
        board.name,
        board.description,
        board.createdAt,
        board.modifiedAt,
        board.deleted
    )

}
