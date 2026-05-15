package com.team01.backend.domain.post.service

import com.team01.backend.domain.post.dto.PostLikeResponseDto
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.entity.PostLike
import com.team01.backend.domain.post.repository.PostLikeRepository
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeService(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val userRepository: UserRepository,
    private val redisTemplate: RedisTemplate<String, String>) {

    // ─────────────────────────────────────────────────────────────
    //  좋아요 토글 (메인 API)
    // ─────────────────────────────────────────────────────────────
    @Transactional
    fun toggleLike(postId: Long, email: String): PostLikeResponseDto {
        val user = findUser(email)
        val post = findPost(postId) // 검증과 동시에 Post 객체 확보

        val inserted = postLikeRepository.tryInsert(user.id, postId)
        val liked = if (inserted > 0) {
            postRepository.increaseLikeCount(postId)
            true
        } else {
            val deleted = postLikeRepository.deleteByUserIdAndPostId(user.id, postId)
            if (deleted > 0) {
                postRepository.decreaseLikeCount(postId)
                false
            } else {
                true
            }
        }
        evictTop5Cache(post.board.id)

        val likeCount = postLikeRepository.countByPostId(postId)
        return PostLikeResponseDto(liked, likeCount)
    }

    // ─────────────────────────────────────────────────────────────
    // 좋아요 수 조회 - DB
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    fun getLikeCount(postId: Long): Int {
        findPost(postId) // 존재 검증
        return postLikeRepository.countByPostId(postId)
    }

    // ─────────────────────────────────────────────────────────────
    //  좋아요 목록 조회 (DB 기준 이력)
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    fun getLikes(postId: Long): List<PostLike> =
        postLikeRepository.findByPostId(postId)


    // ─────────────────────────────────────────────────────────────
    //  내부 헬퍼
    // ─────────────────────────────────────────────────────────────
    private fun findUser(email: String): User =
        userRepository.findByEmail(email)
            .orElseThrow{ EntityNotFoundException("유저를 찾을 수 없어요")}


    private fun findPost(postId: Long): Post =
         postRepository.findByIdOrNull(postId)
            ?: throw EntityNotFoundException("게시글을 찾을 수 없습니다.")


    // 캐시 무효화 로직
    private fun evictTop5Cache(boardId: Long) =
        redisTemplate.delete( "top5:board:$boardId")
}

