package com.team01.backend.domain.post.repository

import com.team01.backend.domain.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface PostRepository : JpaRepository<Post, Long>, PostRepositoryCustom {

    // 전체 게시판별 게시글 수 한 번에 조회 - getAllBoards N+1 해결
    @Query("SELECT p.board.id, COUNT(p) FROM Post p WHERE p.deleted = false GROUP BY p.board.id")
    fun countByBoardGrouped(): List<Array<Any>>

    // 카테고리별 게시글 목록 페이징 조회 (삭제된 게시글 제외)
    @EntityGraph(attributePaths = ["board", "category", "author"])
    fun findAllByBoardIdAndCategoryIdAndDeletedFalse(
        boardId: Long,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Post>

    // 게시글 상세 조회 - board, category, author 한 번에 조회 (N+1 방지)
    @EntityGraph(attributePaths = ["board", "category", "author"])
    fun findWithDetailsById(id: Long): Optional<Post>

    // 게시판별 게시글 수 조회 (삭제된 게시글 제외)
    fun countByBoardIdAndDeletedFalse(boardId: Long): Long

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    fun increaseLikeCount(@Param("id") id: Long)

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    fun decreaseLikeCount(@Param("id") id: Long)

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = :likeCount WHERE p.id = :id")
    fun updateLikeCount(@Param("id") id: Long, @Param("likeCount") likeCount: Int)

    // JOIN FETCH를 사용하여 Author, Category를 한 번에 조회 (N+1 방지)
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        JOIN FETCH p.category
        WHERE p.board.id = :boardId AND p.deleted = false
        ORDER BY p.likeCount DESC, p.createdAt DESC
    """)
    fun findTop5ByBoardId(@Param("boardId") boardId: Long, pageable: Pageable): List<Post>
}