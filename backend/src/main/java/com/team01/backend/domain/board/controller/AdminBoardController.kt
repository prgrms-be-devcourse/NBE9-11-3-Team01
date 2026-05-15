package com.team01.backend.domain.board.controller

import com.team01.backend.domain.board.dto.AdminBoardListResponseDto
import com.team01.backend.domain.board.dto.BoardCreateResponseDto
import com.team01.backend.domain.board.dto.BoardUpdateResponseDto
import com.team01.backend.domain.board.service.BoardService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "관리자 게시판 관리", description = "관리자 게시판 관리 관련 API")
@RestController
@RequestMapping("/admin/boards")
class AdminBoardController(
    private val boardService: BoardService
) {
    // 게시판 생성 request
    data class BoardCreateReqBody(
        @field:NotBlank @field:Size(min = 2, max = 10) val name: String,
        @field:NotBlank @field:Size(min = 5, max = 30) val description: String,
    )

    // 게시판 생성, reqBody에서 문제가 있다면(null, size) globalExceptionHandler에서 처리됨
    @Operation(summary = "게시판 생성", description = "게시판을 생성합니다. 관리자 권한에서만 가능합니다.")
    @PostMapping
    fun createBoard(@RequestBody @Valid  reqBody: BoardCreateReqBody): ResponseEntity<ApiResponse<BoardCreateResponseDto>>{
        val boardCreateResponseDto = boardService.createBoard(reqBody.name, reqBody.description)
        return ResponseEntity.ok(ApiResponse.ofSuccess(
            boardCreateResponseDto
        ))
    }

    // 게시판 수정 request
    data class BoardUpdateReqBody(
        @field:NotBlank @field:Size(min = 2, max = 10) val name: String,
        @field:NotBlank @field:Size(min = 5, max = 30) val description: String
    )

    // 게시판 수정, reqBody에서 문제가 있다면(null, size) globalExceptionHandler에서 처리됨
    @Operation(summary = "게시판 수정", description = "게시판을 수정합니다. 관리자 권한에서만 가능합니다.")
    @PutMapping("/{boardId}")
    fun updateBoard(
        @PathVariable boardId: Long,
        @RequestBody @Valid reqBody: BoardUpdateReqBody
    ):ResponseEntity<ApiResponse<BoardUpdateResponseDto>>{
        val boardUpdateResponseDto = boardService.updateBoard(boardId, reqBody.name, reqBody.description)
        return ResponseEntity.ok(ApiResponse.ofSuccess(
            boardUpdateResponseDto
        ))
    }

    // 게시판 삭제, id만 받아서 삭제
    @Operation(summary = "게시판 삭제", description = "게시판을 Soft Delete 처리합니다. 관리자 권한에서만 가능합니다.")
    @DeleteMapping("/{boardId}")
    fun deleteBoard(
        @PathVariable boardId: Long
    ): ResponseEntity<ApiResponse<Void>>{
        boardService.deleteBoard(boardId)
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody())
    }

    // 게시판 다건 조회
    @Operation(summary = "게시판 조회", description = "삭제된 게시판과 유지중인 게시판을 각각의 리스트로 담아 조회합니다. 관리자 권한에서만 가능합니다.")
    @GetMapping
    fun getBoards(): ResponseEntity<ApiResponse<AdminBoardListResponseDto>> {
        val boards = boardService.getAllBoardsByAdmin()
        return ResponseEntity.ok(ApiResponse.ofSuccess(boards))
    }
}
