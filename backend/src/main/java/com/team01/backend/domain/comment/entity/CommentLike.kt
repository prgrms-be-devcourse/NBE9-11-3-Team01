package com.team01.backend.domain.comment.entity

import com.team01.backend.domain.user.entity.User
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "comment_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_comment_likes_comment_user",
            columnNames = ["comment_id", "user_id"],
        ),
    ],
)
class CommentLike(
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "comment_id", nullable = false)
    val comment: Comment,

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity()
