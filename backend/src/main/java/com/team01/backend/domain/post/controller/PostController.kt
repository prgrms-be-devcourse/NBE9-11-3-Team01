package com.team01.backend.domain.post.controller

import com.team01.backend.domain.post.dto.*
import com.team01.backend.domain.post.service.PostService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@Tag(name = "Post", description = "게시글 API")
@Validated
@RestController
class PostController(
    private val postService: PostService,
) {

    // 검증 로직 분리
    private fun requireLogin(userDetails: UserDetails?): String =
        userDetails?.username
            ?: throw IllegalArgumentException("로그인이 필요한 서비스입니다.")

    // 게시판별 글 목록 조회
    @Operation(summary = "게시글 목록 조회", description = "카테고리별 게시글 목록을 페이징으로 반환합니다.")
    @ApiResponses(value = [SwaggerApiResponse(responseCode = "200", description = "조회 성공")])
    @GetMapping("/boards/{boardId}/posts")
    fun getPostsByBoardId(
        @Parameter(description = "게시판 ID", required = true) @PathVariable boardId: Long,
        @RequestParam(defaultValue = "1") @Min(1) page: Int,
        @RequestParam(required = false) @Size(max = 50) keyword: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "latest") sort: String,
    ): ResponseEntity<ApiResponse<PostPageResponseDto>> =
        ResponseEntity.ok(
            ApiResponse.ofSuccess(
                postService.getPostsByBoardId(boardId, page, keyword, categoryId, sort),
            ),
        )

    // 게시판별, 카테고리별 글 목록 조회
    @Operation(summary = "카테고리별 게시글 목록 조회", description = "특정 게시판/카테고리의 게시글 목록을 페이징으로 반환합니다.")
    @GetMapping("/boards/{boardId}/categories/{categoryId}/posts")
    fun getPostsByCategory(
        @PathVariable boardId: Long,
        @PathVariable categoryId: Long,
        @RequestParam(defaultValue = "1") @Min(1) page: Int,
        @RequestParam(required = false) @Size(max = 50) keyword: String?,
        @RequestParam(defaultValue = "latest") sort: String,
    ): ResponseEntity<ApiResponse<PostPageResponseDto>> =
        ResponseEntity.ok(
            ApiResponse.ofSuccess(
                postService.getPostsByBoardAndCategory(boardId, categoryId, page, keyword, sort),
            ),
        )

    // 인기글 5개 조회 api
    @Operation(summary = "실시간 인기글 조회", description = "게시판별 좋아요 상위 5개 게시글을 조회합니다.")
    @GetMapping("/boards/{boardId}/posts/top5")
    fun getTop5Posts(
        @PathVariable boardId: Long
    ): ResponseEntity<ApiResponse<List<PostResponseDto>>> {

        val posts: List<PostResponseDto> = postService.getTop5Posts(boardId)

        return ResponseEntity.ok(
            ApiResponse.ofSuccess(posts)
        )
    }

    // 게시글 상세 조회
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보와 댓글을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/posts/{postId}")
    fun getPostById(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<PostDetailResponseDto>> {
        val email = requireLogin(userDetails)
        val post = postService.getPostById(postId, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(post))
    }


    data class PostWriteReqBody(
        @field:NotBlank(message = "제목은 공백일 수 없습니다.")
        @field:Size(min = 2, message = "제목은 2자 이상이어야 합니다.")
        val title: String,

        @field:NotBlank(message = "내용은 공백일 수 없습니다.")
        @field:Size(min = 2, message = "내용은 2자 이상이어야 합니다.")
        val content: String,

        @field:NotNull(message = "게시판 선택은 필수입니다.")
        val boardId: Long,

        @field:NotNull(message = "카테고리 선택은 필수입니다.")
        val categoryId: Long
    )

    // 글 작성 api
    @Operation(summary = "게시글 작성", description = "새 게시글을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts")
    fun write(
        @RequestBody @Valid reqBody: PostWriteReqBody,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ApiResponse<PostWriteResponse>> {

        val username = requireLogin(userDetails)

        val post = postService.write(
            username,
            reqBody.title,
            reqBody.content,
            reqBody.boardId,
            reqBody.categoryId
        )

        val postsCount = postService.count()

        return ResponseEntity.ok(
            ApiResponse.ofSuccess(
                PostWriteResponse.of(post, postsCount)
            )
        )
    }


    data class PostModifyReqBody(
        @field:NotBlank(message = "제목은 공백일 수 없습니다.")
        @field:Size(min = 2, message = "제목은 2자 이상이어야 합니다.")
        val title: String,

        @field:NotBlank(message = "내용은 공백일 수 없습니다.")
        @field:Size(min = 2, message = "내용은 2자 이상이어야 합니다.")
        val content: String,

        @field:NotNull(message = "카테고리 선택은 필수입니다.")
        val categoryId: Long
    )

    // 글 수정 api
    @Operation(summary = "게시글 수정", description = "작성자 본인의 게시글을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/posts/{postId}")
    fun modify(
        @PathVariable("postId") postId: Long,
        @RequestBody @Valid reqBody: PostModifyReqBody,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ApiResponse<PostModifyResponse>> {

        val username = requireLogin(userDetails)

        val post = postService.modify(
            postId,
            username,
            reqBody.title,
            reqBody.content,
            reqBody.categoryId
        )

        return ResponseEntity.ok(
            ApiResponse.ofSuccess(PostModifyResponse.of(post))
        )
    }

    // 글 삭제 api
    @Operation(summary = "게시글 삭제", description = "작성자 본인의 게시글을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/posts/{postId}")
    fun delete(
        @PathVariable("postId") postId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ApiResponse<Void>> {

        val username = requireLogin(userDetails)

        postService.delete(postId, username)


        return ResponseEntity.ok(
            ApiResponse.ofSuccessWithoutBody()
        )
    }
}
