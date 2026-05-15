package com.team01.backend.domain.board.controller

import com.team01.backend.domain.board.dto.BoardResponse
import com.team01.backend.domain.board.service.BoardService
import com.team01.backend.domain.category.dto.CategoryResponseDto
import com.team01.backend.domain.category.service.CategoryService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "게시판", description = "게시판 관련 API")
@RestController
@RequestMapping("/boards")
class BoardController(
        private val boardService: BoardService,
        private val categoryService: CategoryService,
        ) {
    // 게시판 목록 조회
    @Operation(summary = "게시판 목록 조회", description = "게시판별 게시글 수 포함")
    @GetMapping
    fun getAllBoards(): ResponseEntity<ApiResponse<List<BoardResponse>>> =
        ResponseEntity.ok(ApiResponse.ofSuccess(ArrayList(boardService.getAllBoards())))

    // 게시판별 카테고리 목록 조회 (비로그인 허용, 글쓰기 페이지 카테고리 선택용)

    @GetMapping("/{boardId}/categories")
    fun getCategoriesByBoard(
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<List<CategoryResponseDto>>> =
        ResponseEntity.ok(ApiResponse.ofSuccess(ArrayList(categoryService.listByBoardId(boardId))))
}