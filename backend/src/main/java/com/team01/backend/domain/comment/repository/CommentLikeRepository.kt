package com.team01.backend.domain.comment.repository

import com.team01.backend.domain.comment.entity.Comment
import com.team01.backend.domain.comment.entity.CommentLike
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface CommentLikeRepository : JpaRepository<CommentLike, Long> {

    /**
     * 좋아요 토글 등 동시 요청 시 likeCount·CommentLike 정합성을 위해 댓글 행을 직렬화합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.id = :id")
    fun findCommentByIdAndPostForUpdate(@Param("id") id: Long): Optional<Comment>

    fun findByComment_IdAndUser_Id(commentId: Long, userId: Long): Optional<CommentLike>

    /** 삭제된 행 수 — 0이면 멱등(이미 없음), 동시성 시 이중 감소 방지에 사용 */
    fun deleteByComment_IdAndUser_Id(commentId: Long, userId: Long): Int

    fun countByComment_Id(commentId: Long): Long

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    fun findLikedCommentIdsByUserId(
        @Param("userId") userId: Long,
        @Param("commentIds") commentIds: List<Long>,
    ): List<Long>
}
