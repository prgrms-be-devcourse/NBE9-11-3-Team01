package com.team01.backend.domain.post.repository

import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.entity.PostLike
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLikeRepository : JpaRepository<PostLike, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p JOIN FETCH p.board WHERE p.id = :id")
    fun findPostByIdForUpdate(@Param("id") id: Long): Post?

    fun findByPost_IdAndUser_Id(postId: Long, userId: Long?): PostLike?

    fun deleteByPost_IdAndUser_Id(postId: Long, userId: Long?): Int

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    fun countByPostId(@Param("postId") postId: Long): Int

    fun findByPostId(postId: Long): List<PostLike>

}