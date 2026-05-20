package com.team01.backend.domain.comment.dto

// COMMENT-02 댓글(답글) 조회 — 응답 전용(작성·수정용 CommentResponseDto와 분리)

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.team01.backend.domain.comment.entity.Comment
import java.time.LocalDateTime

// 게시글 상세 조회 테스트 결과 답글 응답에 replies 빈 배열이 불필요하게 포함되는 문제가 있어서 추가
// 답글의 replies는 항상 빈 배열이라 응답에 불필요 — NON_EMPTY로 빈 배열 필드 제외
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class CommentReadResponseDto(
    val id: Long,
    val content: String,
    val author: String,
    val profileImage: String?,
    val likeCount: Int,
    val deleted: Boolean,
    @get:JsonProperty("isLiked")
    val liked: Boolean,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedAt: LocalDateTime,
    val replies: List<CommentReadResponseDto>,
) {
    companion object {
        // COMMENT-02 댓글(답글) 조회 — 루트 + 답글 엔티티 목록 → 트리 DTO
        fun of(
            root: Comment,
            replyEntities: List<Comment>,
            likedCommentIds: Set<Long>,
        ): CommentReadResponseDto {
            val replyDtos = replyEntities
                .sortedBy { it.createdAt }
                .map { ofReply(it, likedCommentIds) }
            return CommentReadResponseDto(
                id = root.id,
                content = CommentDeleteResponseDto.contentForRead(root),
                author = root.user.nickname,
                profileImage = root.user.profileImage,
                likeCount = root.likeCount,
                deleted = root.deleted,
                liked = likedCommentIds.contains(root.id),
                createdAt = root.createdAt,
                modifiedAt = root.modifiedAt,
                replies = replyDtos,
            )
        }

        fun from(
            root: Comment,
            replyEntities: List<Comment>,
            likedCommentIds: Set<Long>,
        ): CommentReadResponseDto = of(root, replyEntities, likedCommentIds)

        // COMMENT-02 댓글(답글) 조회 — 답글 1건(하위 replies 없음)
        private fun ofReply(reply: Comment, likedCommentIds: Set<Long>): CommentReadResponseDto =
            CommentReadResponseDto(
                id = reply.id,
                content = CommentDeleteResponseDto.contentForRead(reply),
                author = reply.user.nickname,
                profileImage = reply.user.profileImage,
                likeCount = reply.likeCount,
                deleted = reply.deleted,
                liked = likedCommentIds.contains(reply.id),
                createdAt = reply.createdAt,
                modifiedAt = reply.modifiedAt,
                replies = emptyList(),
            )
    }
}
