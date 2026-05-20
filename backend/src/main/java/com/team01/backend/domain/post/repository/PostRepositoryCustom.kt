package com.team01.backend.domain.post.repository

import com.team01.backend.domain.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

// 게시글 검색 커스텀 쿼리 인터페이스 (QueryDSL 사용을 위한 확장)
interface PostRepositoryCustom {
    fun searchByBoardId(
            boardId: Long,
            keyword: String?,
            categoryId: Long?,
            pageable: Pageable,
            sort: String,
            ): Page<Post>
}
