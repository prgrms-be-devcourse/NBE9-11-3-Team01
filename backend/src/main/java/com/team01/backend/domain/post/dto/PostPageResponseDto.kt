package com.team01.backend.domain.post.dto

import kotlin.jvm.JvmStatic
import org.springframework.data.domain.Page

data class PostPageResponseDto(
        val posts: List<PostResponseDto>,
        val currentPage: Int,
        val totalPages: Int,
        val totalElements: Long,
        val hasNext: Boolean,
) {
    companion object {
        @JvmStatic
        fun of(page: Page<PostResponseDto>): PostPageResponseDto = PostPageResponseDto(
                posts = page.content,
                currentPage = page.number + 1,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                hasNext = page.hasNext(),
                )

        @JvmStatic
        fun from(page: Page<PostResponseDto>): PostPageResponseDto = of(page)
    }
}