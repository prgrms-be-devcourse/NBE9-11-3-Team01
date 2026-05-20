package com.team01.backend.domain.post.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.category.entity.Category
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

import java.time.LocalDateTime
import kotlin.jvm.java

// PostRepositoryCustom 구현체 - QueryDSL로 동적 검색 쿼리 처리
@Repository
class PostRepositoryImpl(
        private val queryFactory: JPAQueryFactory,
        ) : PostRepositoryCustom {
    private val post = PathBuilder(Post::class.java, "post")
    private val author = PathBuilder(User::class.java, "author")
    private val category = PathBuilder(Category::class.java, "category")

    override fun searchByBoardId(
            boardId: Long,
            keyword: String?,
            categoryId: Long?,
            pageable: Pageable,
            sort: String,
    ): Page<Post> {
        val posts = queryFactory
            .selectFrom(post)
            .join(post.get("author", User::class.java), author).fetchJoin()
            .join(post.get("category", Category::class.java), category).fetchJoin()
            .where(
                boardIdEq(boardId),
                post.getBoolean("deleted").eq(false),
                containsKeyword(keyword),
                eqCategoryId(categoryId),
            )
            .orderBy(
                if (sort == "likes") {
                    post.getNumber("likeCount", java.lang.Integer::class.java).desc()
                } else {
                    post.getDateTime("createdAt", LocalDateTime::class.java).desc()
                },
            )
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .toList()

        val total: Long = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        boardIdEq(boardId),
                    post.getBoolean("deleted").eq(false),
                        containsKeyword(keyword),
                        eqCategoryId(categoryId),
                        )
                .fetchOne() ?: 0L

        return PageImpl(posts, pageable, total)
    }

    private fun boardIdEq(boardId: Long): BooleanExpression =
        post.get("board", Board::class.java)
            .getNumber("id", java.lang.Long::class.java)
            .eq(boardId as java.lang.Long)

    private fun eqCategoryId(categoryId: Long?): BooleanExpression? =
        categoryId?.let {
            post.get("category", Category::class.java)
                .getNumber("id", java.lang.Long::class.java)
                .eq(it as java.lang.Long)
        }

    // sanitize 로직 제거, Filter에서 이미 처리됨
    private fun containsKeyword(keyword: String?): BooleanExpression? {
    if (keyword == null) return null
    if (keyword.isBlank()) return Expressions.FALSE
    return post.getString("title").containsIgnoreCase(keyword.trim())
    }
}