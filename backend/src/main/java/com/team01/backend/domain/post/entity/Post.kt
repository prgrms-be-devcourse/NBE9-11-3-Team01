package com.team01.backend.domain.post.entity

import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.category.entity.Category
import com.team01.backend.domain.comment.entity.Comment
import com.team01.backend.domain.user.entity.User
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.*;

@Entity
@Table(
    indexes = [
        Index(// 게시판별 게시글 목록 조회 최적화 (board_id + deleted 필터링, createdAt 정렬)
            name = "idx_post_board_deleted_created",
            columnList = "board_id, isDeleted, createdAt",
        ),
    ],
)
class Post : BaseEntity {
    lateinit var title: String
        protected set

    //private String HtmlStyles.title;

    @field:Column(columnDefinition = "TEXT")
    lateinit var content: String
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    lateinit var board: Board
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    lateinit var author: User
        protected set

    var likeCount: Int = 0
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    lateinit var category: Category
        protected set

    @field:Column(name = "isDeleted")
    var deleted: Boolean = false
        protected set

    @field:OneToMany(
        mappedBy = "post",
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        fetch = FetchType.LAZY,
        orphanRemoval = false,
    )
    var comments: MutableList<Comment> = mutableListOf()
        protected set

    protected constructor()

    constructor(author: User, title: String, content: String, board: Board, category: Category) {
        this.title = title
        this.content = content
        this.board = board
        this.author = author
        this.category = category
    }

    fun update(title: String, content: String, category: Category) {
        this.title = title
        this.content = content
        this.category = category
    }

    fun delete() {
        this.deleted = true
    }

    // top5 조회를 위한 임시 메서드
    fun initLikeCount(likeCount: Int) {
        this.likeCount = likeCount
    }

    fun incrementLikeCount() {
        likeCount++
    }

    fun decrementLikeCount() {
        if (likeCount > 0) likeCount--
    }
}
