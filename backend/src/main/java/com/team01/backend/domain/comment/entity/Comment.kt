package com.team01.backend.domain.comment.entity

import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.user.entity.User
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "comments")
class Comment : BaseEntity {

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "post_id", nullable = false)
    lateinit var post: Post
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        protected set

    @Column(nullable = false)
    lateinit var content: String
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "parent_id")
    var parent: Comment? = null // 대댓글이면 부모 댓글, 일반 댓글이면 null
        protected set

    @field:Column(name = "isDeleted")
    var deleted: Boolean = false
        protected set

    var likeCount: Int = 0
        protected set

    protected constructor()

    // 댓글 생성할 때 쓰는 생성자
    constructor(post: Post, user: User, content: String, parent: Comment?) {
        this.post = post
        this.user = user
        this.content = content
        this.parent = parent
    }

    // 초기 데이터용 생성자 추가
    constructor(post: Post, content: String, parent: Comment?) {
        this.post = post
        this.content = content
        this.parent = parent // null이면 일반 댓글, 값 있으면 대댓글
    }

    // 댓글 수정할 때 쓰는 메서드
    fun update(content: String) {
        this.content = content
    }

    /** 소프트 삭제는 deleted만 변경. 호출부는 COMMENT-04 댓글 삭제. */
    fun softDelete() {
        this.deleted = true
    }

    fun isDeleted(): Boolean = deleted

    /** 좋아요 추가 시 CommentLike 저장과 함께 호출. */
    fun incrementLikeCount() {
        this.likeCount++
    }

    /** 좋아요 취소 시 CommentLike 삭제와 함께 호출. */
    fun decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }
}
