package com.team01.backend.domain.post.entity

import com.team01.backend.domain.user.entity.User
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.*

/**
 * 좋아요 이력 테이블
 * 
 * ✅ UNIQUE(user_id, post_id) 제약:
 * 동시 요청으로 중복 INSERT 시도가 오더라도 DB 레벨에서 차단.
 * 서비스 레이어의 exists 체크 + save 사이의 Race Condition 방어.
 */
@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_post_likes_user_post",
        columnNames = ["user_id", "post_id"]
    )]
)
class PostLike(
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "post_id", nullable = false)
    val post: Post
) : BaseEntity()
