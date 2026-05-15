package com.team01.backend.domain.board.dto

data class AdminBoardListResponseDto(
   val exist: List<AdminBoardResponseDto>,
   val deleted: List<AdminBoardResponseDto>
)
