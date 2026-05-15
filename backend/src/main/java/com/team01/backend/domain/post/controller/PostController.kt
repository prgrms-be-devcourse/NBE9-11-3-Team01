package com.team01.backend.domain.post.controller

import com.team01.backend.domain.post.dto.*
import com.team01.backend.domain.post.service.PostService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@Tag(name = "게시글", description = "게시글 관련 API")
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
    @Operation(summary = "게시판별 글 목록 조회", description = "키워드 검색, 카테고리 필터, 페이징 지원")
    @GetMapping("/boards/{boardId}/posts")
    fun getPostsByBoardId(
        @PathVariable boardId: Long,
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
    @Operation(summary = "게시판별-카테고리별 글 목록 조회", description = "키워드 검색, 페이징 지원")
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

    /*// 인기글 5개 조회 api
    @Operation(summary = "실시간 인기글 조회", description = "게시판별 \"좋아요\"를 많이 받은 5개의 게시물 노출")
    @GetMapping("boards/{boardId}/posts/top5")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getTop5Posts(
            @PathVariable Long boardId
    ) {
        List<PostResponseDto> posts = postService.getTop5Posts(boardId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(posts));
    }*/

    // 게시글 상세 조회
    @Operation(summary = "게시글 상세 조회", description = "비로그인 사용자 접근 불가, 작성자 여부 포함")
    @GetMapping("/posts/{postId}")
    fun getPostById(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: UserDetails?,
    ): ResponseEntity<ApiResponse<PostDetailResponseDto>> {
        val email = requireLogin(userDetails)
        val post = postService.getPostById(postId, email)
        return ResponseEntity.ok(ApiResponse.ofSuccess(post))
    }


    /*record PostWriteReqBody(
            @Size(min = 2, message = "제목은 2자 이상이어야 합니다.")
            @NotBlank(message = "제목은 공백일 수 없습니다.")
            String title,

            @Size(min = 2, message = "내용은 2자 이상이어야 합니다.")
            @NotBlank(message = "내용은 공백일 수 없습니다.")
            String content,

            @NotNull(message = "게시판 선택은 필수입니다.")
            Long boardId,

            @NotNull(message = "카테고리 선택은 필수입니다.")
            Long categoryId
    ){
    }*/

    /*// 글 작성 api
    @Operation(summary = "글 작성", description = "비로그인 사용자 접근 불가")
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostWriteResponse>> write(
            @RequestBody @Valid PostWriteReqBody reqBody,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        // 비로그인 사용자에 대한 예외 처리
        if (userDetails == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Post post = postService.write(
                userDetails.getUsername(),
                reqBody.title,
                reqBody.content,
                reqBody.boardId,
                reqBody.categoryId
        );

        long postsCount = postService.count();

        return ResponseEntity.ok(
                ApiResponse.ofSuccess(
                       PostWriteResponse.of(post, postsCount)
                )
        );
    }*/

    /*record PostModifyReqBody(
            @Size(min = 2, message = "제목은 2자 이상이어야 합니다.")
            @NotBlank(message = "제목은 공백일 수 없습니다.")
            String title,

            @Size(min = 2, message = "내용은 2자 이상이어야 합니다.")
            @NotBlank(message = "내용은 공백일 수 없습니다.")
            String content,

            @NotNull(message = "카테고리 선택은 필수입니다.")
            Long categoryId
    ) {
    }*/

    /*// 글 수정 api
    @Operation(summary = "글 수정", description = "비로그인 사용자 접근 불가, 원글 작성자만 수정 가능")
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostModifyResponse>> modify(
            @PathVariable("postId") Long postId,
            @RequestBody @Valid PostModifyReqBody reqBody,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        if (userDetails == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Post post = postService.modify(
                postId,
                userDetails.getUsername(),
                reqBody.title(),
                reqBody.content(),
                reqBody.categoryId);

        return ResponseEntity.ok(
                ApiResponse.ofSuccess(PostModifyResponse.Companion.of(post))
        );
    }*/

    /*// 글 삭제 api
    @Operation(summary = "글 삭제", description = "비로그인 사용자 접근 불가, 원글 작성자만 삭제 가능")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        if (userDetails == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        postService.delete(postId, userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.ofSuccess(null)
        );
    }*/
}
