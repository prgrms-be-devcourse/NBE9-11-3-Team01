package com.team01.backend.domain.post.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.domain.category.repository.CategoryRepository
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.domain.post.dto.PostDetailResponseDto
import com.team01.backend.domain.post.dto.PostPageResponseDto
import com.team01.backend.domain.post.dto.PostResponseDto
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.repository.PostLikeRepository
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration




@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val commentService: CommentService,
    private val userRepository: UserRepository,
    private val boardRepository: BoardRepository,
    private val categoryRepository: CategoryRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val postLikeRepository: PostLikeRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 페이징 공통 메서드(헬퍼 메서드)
    private fun toPageable(page: Int): Pageable =
        PageRequest.of(page - 1, PAGE_SIZE, Sort.by("createdAt").descending())

    // 조회 시 검증 로직 따로 분리
    private fun validatePost(post: Post) {
        if (post.deleted) {
            throw EntityNotFoundException("존재하지 않는 게시글입니다.")
        }

        val board: Board = post.board

        if (board.deleted) {
            throw EntityNotFoundException("존재하지 않는 게시판입니다.")
        }
    }

    // 카테고리가 해당 게시판 소속인지 검증
    private fun validateCategoryInBoard(categoryId: Long, boardId: Long) {
        val category = categoryRepository.findByIdOrNull(categoryId)
            ?: throw EntityNotFoundException("카테고리를 찾을 수 없습니다.")

        if (category.boardId != boardId) {
            throw IllegalArgumentException("해당 게시판에서 사용할 수 없는 카테고리입니다.")
        }
    }


    @Transactional
    fun write(
        email: String,
        title: String,
        content: String,
        boardId: Long,
        categoryId: Long
    ): Post {
        val author = userRepository.findByEmail(email)
            //TODO UserRepository.findByEmail optional 제거 되면 .orElseThrow 지우고, 아래 주석 코드로 변경
            //?: throw EntityNotFoundException("사용자를 찾을 수 없습니다.")
            .orElseThrow { EntityNotFoundException("사용자를 찾을 수 없습니다.") }

        // 게시판, 카테고리 조회
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw EntityNotFoundException("존재하지 않는 게시판입니다.")

        // 검증 메서드 실행 (여기서 실제 DB 조회 / 게시판-카테고리 소속 검증 완료)
        validateCategoryInBoard(categoryId, boardId)

        // 레포지토리 수정 없이 내장 메서드로 객체 확보 (추가 쿼리 발생 X)
        val category = categoryRepository.getReferenceById(categoryId)

        val post = Post(
            author = author,
            title = title,
            content = content,
            board = board,
            category = category
        )

        val savedPost = postRepository.save(post)

        // 캐시 무효화 (board.id 검증 포함)
        val targetBoardId = savedPost.board.id
            ?: throw IllegalStateException("Board id is null")
        evictTop5Cache(targetBoardId)

        return savedPost
    }

    fun count(): Long = postRepository.count()

    // 게시판별 게시글 목록 페이징 조회 (키워드 검색, 카테고리 필터 포함)
    fun getPostsByBoardId(
        boardId: Long,
        page: Int,
        keyword: String?,
        categoryId: Long?,
        sort: String,
    ): PostPageResponseDto {
        boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw EntityNotFoundException("존재하지 않는 게시판입니다.")

        val postPage = postRepository
            .searchByBoardId(boardId, keyword, categoryId, toPageable(page), sort)
            .map { PostResponseDto.of(it) }

        return PostPageResponseDto.from(postPage)
    }

    // 게시글 상세 조회 (비로그인 사용자는 Controller에서 차단, 작성자 여부 포함)
    fun getPostById(postId: Long, email: String): PostDetailResponseDto {
        val post = postRepository.findWithDetailsById(postId)
            ?: throw EntityNotFoundException("존재하지 않는 게시글입니다.")

        validatePost(post)

        val currentUser = userRepository.findByEmail(email)
            ?.let { if (it.isPresent) it.get() else null }
        val isOwner = currentUser != null && post.author.id == currentUser.id
        val liked = currentUser != null &&
                postLikeRepository.findByUserIdAndPostId(
                    currentUser.id ?: throw IllegalStateException("사용자 ID가 없습니다."),
                    postId
                ) != null
        val comments = commentService.getCommentsByPostId(postId, currentUser?.email)

        return PostDetailResponseDto.of(post, post.board, post.category, comments, isOwner, liked)
    }

    fun findById(id: Long): Post? = postRepository.findByIdOrNull(id)

    @Transactional
    fun modify(
        postId: Long,
        email: String,
        title: String,
        content: String,
        categoryId: Long
    ): Post {
        // 게시글 및 요청자(actor) 조회
        val post = postRepository.findByIdOrNull(postId)
            ?: throw EntityNotFoundException("게시글을 찾을 수 없습니다.")

        //TODO UserRepository.findByEmail optional 제거 되면 .orElseThrow 지우고, 아래 주석 코드로 변경
        // val actor = userRepository.findByEmail(email) ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다.")
        val actor = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("사용자를 찾을 수 없습니다.") }


        // 작성자 권한 검증 (컨벤션 8번)
        if (post.author.id != actor.id) {
            throw AccessDeniedException("작성자만 수정할 수 있습니다.")
        }

        // 변경할 카테고리 조회 및 게시판 소속 검증
        val category = categoryRepository.findByIdOrNull(categoryId)
            ?: throw EntityNotFoundException("카테고리를 찾을 수 없습니다.")

        if (category.boardId != post.board.id) {
            throw IllegalArgumentException("해당 게시판에서 사용할 수 없는 카테고리입니다.")
        }

        post.update(title, content, category)

        return post
    }


    @Transactional
    fun delete(postId: Long, email: String) {
        // 게시글 조회
        val post = postRepository.findByIdOrNull(postId)
            ?: throw EntityNotFoundException("해당 게시물을 찾을 수 없습니다.")

        // 요청 유저(actor) 조회
        //TODO UserRepository.findByEmail optional 제거 되면 .orElseThrow 지우고, 아래 주석 코드로 변경
        // val actor = userRepository.findByEmail(email) ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다.")
        val actor = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("사용자를 찾을 수 없습니다.") }

        // 삭제 여부 확인 (컨벤션 2번: isDeleted 대신 deleted 프로퍼티 사용)
        if (post.deleted) {
            throw IllegalArgumentException("이미 삭제된 게시물입니다.")
        }

        // 권한 검증
        if (post.author.id != actor.id) {
            throw AccessDeniedException("작성자만 삭제할 수 있습니다.")
        }

        post.delete()

        // 캐시 무효화
        val targetBoardId = post.board.id
            ?: throw IllegalStateException("Board id is null")

        evictTop5Cache(targetBoardId)
    }


    fun getPostsByBoardAndCategory(
        boardId: Long,
        categoryId: Long,
        page: Int,
        keyword: String?,
        sort: String,
    ): PostPageResponseDto {
        validateCategoryInBoard(categoryId, boardId)
        val postPage = postRepository
            .searchByBoardId(boardId, keyword, categoryId, toPageable(page), sort)
            .map { PostResponseDto.of(it) }

        return PostPageResponseDto.from(postPage)
    }

    fun getTop5Posts(boardId: Long): List<PostResponseDto> {
        val cacheKey = "top5:board:$boardId"

        // 캐시 조회
        val cachedJson = redisTemplate.opsForValue().get(cacheKey)

        try {
            cachedJson?.let {
                return objectMapper.readValue(it, object : TypeReference<List<PostResponseDto>>() {})
            }

            // 캐시 부재 시 DB 조회 및 응답 변환
            val posts = postRepository.findTop5ByBoardId(boardId, PageRequest.of(0, 5))
            val response = posts.map { PostResponseDto.of(it) }

            // Redis 저장
            val json = objectMapper.writeValueAsString(response)
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10))

            return response

        } catch (e: JsonProcessingException) {
            log.error("인기글 캐시 처리 중 오류 발생: {}", e.message)
            // 예외 발생 시 DB 데이터만 반환 (컨벤션 8번: stream().map() 대신 map {} 활용)
            return postRepository.findTop5ByBoardId(boardId, PageRequest.of(0, 5))
                .map { PostResponseDto.of(it) }
        }
    }


    // redis 캐시 삭제 (글 등록, 글 삭제, likeCount 변경 메서드에서 사용됨)
    private fun evictTop5Cache(boardId: Long) {
        val cacheKey = "top5:board:$boardId"
        redisTemplate.delete(cacheKey)
        log.info("캐시 무효화 완료: {}", cacheKey)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
