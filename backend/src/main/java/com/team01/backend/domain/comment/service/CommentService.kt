package com.team01.backend.domain.comment.service

import com.team01.backend.domain.comment.dto.CommentDeleteResponseDto
import com.team01.backend.domain.comment.dto.CommentLikeToggleResponseDto
import com.team01.backend.domain.comment.dto.CommentReadResponseDto
import com.team01.backend.domain.comment.dto.CommentRequestDto
import com.team01.backend.domain.comment.dto.CommentResponseDto
import com.team01.backend.domain.comment.entity.Comment
import com.team01.backend.domain.comment.entity.CommentLike
import com.team01.backend.domain.comment.repository.CommentLikeRepository
import com.team01.backend.domain.comment.repository.CommentRepository
import com.team01.backend.domain.notification.event.CommentCreatedEvent
import com.team01.backend.domain.notification.event.ReplyCreatedEvent
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    // 댓글 수 조회
    fun count(): Long = commentRepository.count()

    // 초기 데이터용
    @Transactional
    fun writeInitComment(postId: Long, tempUser: User, content: String, parentId: Long?) {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw EntityNotFoundException("게시글을 찾을 수 없습니다.")
        if (post.deleted) {
            throw EntityNotFoundException("게시글을 찾을 수 없습니다.")
        }

        // parentId 있으면 부모 댓글 찾기, 없으면 null
        val parent = parentId?.let { id ->
            commentRepository.findByIdOrNull(id)
                ?: throw EntityNotFoundException("부모 댓글을 찾을 수 없습니다.")
        }

        commentRepository.save(Comment(post, tempUser, content, parent))
    }

    /** 로그인 사용자 조회 — 로그인 검증은 Controller, 여기서는 DB 사용자 로딩만 */
    private fun findUser(email: String): User =
        userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("유저를 찾을 수 없습니다.") }

    //-----------------------------------------------------------------------------------------------------------------

    // COMMENT-02 댓글(답글) 조회 — email은 Controller(또는 PostService)에서 전달, null이면 좋아요 여부 미표시
    @Transactional(readOnly = true)
    fun getCommentsByPostId(postId: Long, email: String?): List<CommentReadResponseDto> {
        val userId = email?.let { findUser(it).id }

        // 기존 코드 유지
        val roots = commentRepository.findByPost_IdAndParentIsNullOrderByCreatedAtAsc(postId)
        if (roots.isEmpty()) return emptyList()

        val rootIds = roots.map { it.id }
        val allReplies = commentRepository.findByParent_IdInOrderByCreatedAtAsc(rootIds)

        // 모든 댓글 ID 수집
        val allCommentIds = buildList {
            addAll(rootIds)
            addAll(allReplies.map { it.id })
        }

        // 좋아요한 댓글 ID Set 조회
        val likedCommentIds: Set<Long> = userId?.let { uid ->
            commentLikeRepository.findLikedCommentIdsByUserId(uid, allCommentIds).toHashSet()
        } ?: emptySet()

        val repliesByParentId = allReplies.groupBy { reply ->
            requireNotNull(reply.parent?.id) { "답글은 부모 댓글이 필요합니다" }
        }

        return roots.map { root ->
            CommentReadResponseDto.of(
                root,
                repliesByParentId[root.id].orEmpty(),
                likedCommentIds,
            )
        }
    }

    @Transactional
    fun writeComment(postId: Long, reqDto: CommentRequestDto, email: String): CommentResponseDto {
        val user = findUser(email)

        // 게시글 존재 확인
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("게시글을 찾을 수 없습니다.") }

        // 삭제된 게시글 확인 -> code:400
        if (post.deleted) {
            throw IllegalArgumentException("삭제된 게시글에는 댓글을 달 수 없습니다.")
        }

        // 대댓글인지 확인 부모 댓글 없을 시 예외처리 -> code : 404
        val parent: Comment? = reqDto.parentId?.let { id ->
            commentRepository.findById(id)
                .orElseThrow { EntityNotFoundException("부모 댓글을 찾을 수 없습니다.") }
                .also {
                    if (it.post.id != postId) throw IllegalArgumentException("잘못된 게시글의 댓글입니다.")
                    if (it.deleted) throw IllegalArgumentException("삭제된 댓글에는 답글을 달 수 없습니다.")
                    if (it.parent != null) throw IllegalArgumentException("답글에는 답글을 달 수 없습니다.")
                }
        }

        val comment = Comment(post, user, reqDto.content, parent)
        commentRepository.save(comment)

        // 댓글 달림 이벤트 발행
        val maxLength = 10 // 알림에서 보여줄 내용 글자수 제한
        val contentLimit = comment.content.let { content ->
            if (content.length > maxLength) content.take(maxLength) + "..." else content
        }

        if (post.author.id != user.id) { // 자기 글에 댓글 작성한 경우에는 발행하지 X
            eventPublisher.publishEvent(
                CommentCreatedEvent(
                    postId,
                    post.author.id,
                    comment.id,
                    user.id,
                    contentLimit,
                ),
            )
        }

        // 답글이라면 답글 생성 이벤트도 발행 + 동일인이라면 발행하지 X
        if (parent != null && parent.user.id != user.id) {
            eventPublisher.publishEvent(
                ReplyCreatedEvent(
                    postId,
                    parent.id,
                    parent.user.id,
                    comment.id,
                    user.id,
                    contentLimit,
                ),
            )
        }

        return CommentResponseDto.of(comment)
    }

    @Transactional
    fun updateComment(commentId: Long, reqDto: CommentRequestDto, email: String): CommentResponseDto {
        val user = findUser(email)

        // 댓글 존재 확인 -> code:404
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw EntityNotFoundException("댓글을 찾을 수 없습니다.")

        if (comment.deleted) throw IllegalArgumentException("삭제된 댓글은 수정할 수 없습니다.")
        if (comment.user.id != user.id) throw IllegalArgumentException("본인 댓글만 수정할 수 있습니다.")

        comment.update(reqDto.content)
        return CommentResponseDto.of(comment)
    }

    /*
     * ============================================================================================================
     * COMMENT-04 댓글(답글) 삭제 — 소프트 딜리트(deleted), 본인만 삭제
     * 검증 순서: 존재 → 댓글 삭제 여부 → 게시글 삭제 여부 → 작성자 권한
     * (인가 실패는 IllegalArgumentException이 아닌 AccessDeniedException)
     * ============================================================================================================
     */
    @Transactional
    fun deleteComment(commentId: Long, email: String): CommentDeleteResponseDto {
        val loginUser = findUser(email)

        val comment = commentRepository.findByIdWithPost(commentId) ?: throw EntityNotFoundException("댓글을 찾을 수 없습니다.")

        if (comment.deleted) throw IllegalArgumentException("이미 삭제된 댓글입니다.")

        if (comment.post.deleted) throw EntityNotFoundException("게시글을 찾을 수 없습니다.")

        if (comment.user.id != loginUser.id) throw AccessDeniedException("본인 댓글만 삭제할 수 있습니다.")

        comment.softDelete()
        commentRepository.saveAndFlush(comment)
        return CommentDeleteResponseDto.of(commentId)
    }

    /*
     * ============================================================================================================
     * 댓글 좋아요 토글 — 미등록 시 좋아요, 이미 누른 상태면 취소 (인스타/유튜브 댓글과 동일 UX).
     * 동시에 같은 댓글을 누를 때 likeCount 경쟁 상태를 막기 위해 댓글 행에 비관적 락을 겁니다.
     * ============================================================================================================
     */
    @Transactional
    fun toggleCommentLike(commentId: Long, email: String): CommentLikeToggleResponseDto {
        val user = findUser(email)

        val comment = commentLikeRepository.findCommentByIdAndPostForUpdate(commentId)
            ?: throw  EntityNotFoundException("댓글을 찾을 수 없습니다.")

        if (comment.deleted) {
            throw IllegalArgumentException("삭제된 댓글에는 좋아요를 할 수 없습니다.")
        }
        if (comment.post.deleted) {
            throw EntityNotFoundException("게시글을 찾을 수 없습니다.")
        }

        val removed = commentLikeRepository.deleteByComment_IdAndUser_Id(commentId, user.id)
        if (removed > 0) {
            comment.decrementLikeCount()
            return CommentLikeToggleResponseDto(commentId, comment.likeCount, false)
        }

        commentLikeRepository.save(CommentLike(comment, user))
        comment.incrementLikeCount()
        return CommentLikeToggleResponseDto(commentId, comment.likeCount, true)
    }
}
