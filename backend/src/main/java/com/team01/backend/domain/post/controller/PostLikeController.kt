package com.team01.backend.domain.post.controller

import com.team01.backend.domain.post.dto.PostLikeResponseDto
import com.team01.backend.domain.post.service.PostLikeService
import com.team01.backend.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "게시글 좋아요", description = "게시글 좋아요 관련 API")
@RestController
class PostLikeController(
    private val postLikeService: PostLikeService
) {

    @Operation(summary = "게시글 좋아요 토글", description = "게시글 좋아요를 누르거나 취소합니다. 한 번 누르면 좋아요, 다시 누르면 취소됩니다.")
    @PostMapping("/posts/{postId}/likes")
    fun toggleLike(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<PostLikeResponseDto>> =
        ResponseEntity.ok(
            ApiResponse.ofSuccess(
                postLikeService.toggleLike(postId, userDetails.username)
            )
        )


    @Hidden
    @GetMapping("/posts/{postId}/likes")
    fun getLikesCount(
        @PathVariable postId: Long
    ): ResponseEntity<ApiResponse<Int>> =
        ResponseEntity.ok(
            ApiResponse.ofSuccess(
                postLikeService.getLikeCount(postId)
            )
        )
}
