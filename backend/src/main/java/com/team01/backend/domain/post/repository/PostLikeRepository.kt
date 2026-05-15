package com.team01.backend.domain.post.repository

import com.team01.backend.domain.post.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    @Query("SELECT pl FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id = :postId")
    fun findByUserIdAndPostId(@Param("userId") userId: Long, @Param("postId") postId: Long): PostLike?

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    fun countByPostId(@Param("postId") postId: Long): Int

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id = :postId")
    fun deleteByUserIdAndPostId(@Param("userId") userId: Long, @Param("postId") postId: Long): Int

    @Modifying
    @Query(
        value = "INSERT INTO POST_LIKES (USER_ID, POST_ID, CREATEDAT, MODIFIEDAT) " +
                "SELECT :userId, :postId, NOW(), NOW() " +
                "WHERE NOT EXISTS " +
                "(SELECT 1 FROM POST_LIKES WHERE USER_ID = :userId AND POST_ID = :postId)",
        nativeQuery = true
    )
    fun tryInsert(@Param("userId") userId: Long, @Param("postId") postId: Long): Int

    fun findByPostId(postId: Long): List<PostLike>
}