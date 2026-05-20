package com.team01.backend.domain.comment.controller

import com.team01.backend.domain.comment.dto.CommentDeleteResponseDto
import com.team01.backend.domain.comment.dto.CommentLikeToggleResponseDto
import com.team01.backend.domain.comment.dto.CommentReadResponseDto
import com.team01.backend.domain.comment.dto.CommentRequestDto
import com.team01.backend.domain.comment.dto.CommentResponseDto
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*


@Tag(name = "Comment", description = "댓글 API")
@RestController
class CommentController(
    private val commentService: CommentService,
) {

    // 로그인 검증 — PostController와 동일하게 컨트롤러에서 처리
    private fun validateLogin(userDetails: UserDetails?): String =
        userDetails?.username
            ?: throw IllegalArgumentException("로그인이 필요한 서비스입니다.")

    // COMMENT-02 댓글(답글) 조회
    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 댓글/대댓글 목록을 조회합니다.")
    @ApiResponses(value = [SwaggerApiResponse(responseCode = "200", description = "조회 성공"), SwaggerApiResponse(responseCode = "401", description = "인증 필요")])
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/posts/{postId}/comments")
    fun getComments(
        @Parameter(description = "게시글 ID", required = true) @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<List<CommentReadResponseDto>>> {
        val email = validateLogin(userDetails)
        val list = commentService.getCommentsByPostId(postId, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(list))
    }

    @Operation(summary = "댓글 작성", description = "특정 게시글에 댓글 또는 대댓글을 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts/{postId}/comments")
    fun writeComment(
        @PathVariable postId: Long,
        @Valid @RequestBody reqDto: CommentRequestDto,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<CommentResponseDto>> {
        val email = validateLogin(userDetails)
        val resDto = commentService.writeComment(postId, reqDto, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(resDto))
    }

    @Operation(summary = "댓글 수정", description = "작성한 댓글을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/comments/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @Valid @RequestBody requestDto: CommentRequestDto,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<CommentResponseDto>> {
        val email = validateLogin(userDetails)
        val resDto = commentService.updateComment(commentId, requestDto, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(resDto))
    }

    /** 댓글 좋아요 토글 — 한 번 호출 시 좋아요, 한 번 더 호출 시 취소 */
    @Operation(summary = "댓글 좋아요 토글", description = "댓글 좋아요를 등록/취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/comments/{commentId}/likes")
    fun toggleCommentLike(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<CommentLikeToggleResponseDto>> {
        val email = validateLogin(userDetails)
        val dto = commentService.toggleCommentLike(commentId, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(dto))
    }

    // COMMENT-04 댓글(답글) 삭제 — DELETE, 소프트 딜리트(서비스에서 deleted 처리)
    @Operation(summary = "댓글 삭제", description = "작성한 댓글을 소프트 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<CommentDeleteResponseDto>> {
        val email = validateLogin(userDetails)
        val body = commentService.deleteComment(commentId, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(body))
    }
}
