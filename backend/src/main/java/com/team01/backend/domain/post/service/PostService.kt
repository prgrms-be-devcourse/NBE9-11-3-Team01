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
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.Optional


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
        if (board.isDeleted) {
            throw EntityNotFoundException("존재하지 않는 게시판입니다.")
        }
    }

    // 카테고리가 해당 게시판 소속인지 검증
    private fun validateCategoryInBoard(categoryId: Long, boardId: Long) {
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { EntityNotFoundException("카테고리를 찾을 수 없습니다.") }

        if (category.boardId != boardId) {
            throw IllegalArgumentException("해당 게시판에서 사용할 수 없는 카테고리입니다.")
        }
    }


    /*@Transactional
    public Post write(String email, String title, String content, Long boardId, Long categoryId) {

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 게시판, 카테고리 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 게시판입니다."));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 카테고리입니다."));

        Post post = new Post(author, HtmlStyles.title, content, board, category);

        evictTop5Cache(post.getBoard().getId());

        return postRepository.save(post);
    }*/

    fun count(): Long = postRepository.count()

    // 게시판별 게시글 목록 페이징 조회 (키워드 검색, 카테고리 필터 포함)
    fun getPostsByBoardId(
        boardId: Long,
        page: Int,
        keyword: String?,
        categoryId: Long?,
        sort: String,
    ): PostPageResponseDto {
        val postPage = postRepository
            .searchByBoardId(boardId, keyword, categoryId, toPageable(page), sort)
            .map { PostResponseDto.of(it) }

        return PostPageResponseDto.from(postPage)
    }

    // 게시글 상세 조회 (비로그인 사용자는 Controller에서 차단, 작성자 여부 포함)
    fun getPostById(postId: Long, email: String?): PostDetailResponseDto {
        val post = postRepository.findWithDetailsById(postId)
            .orElseThrow { EntityNotFoundException("존재하지 않는 게시글입니다.") }

        validatePost(post)

        val currentUser = email
            ?.let { userRepository.findByEmail(it).orElse(null) }
        val isOwner = currentUser != null && post.author.id == currentUser.id
        val isLiked = currentUser != null &&
                postLikeRepository.findByUserIdAndPostId(currentUser.id, postId).isPresent
        val comments = commentService.getCommentsByPostId(postId, currentUser?.email)

        return PostDetailResponseDto.of(post, post.board, post.category, comments, isOwner, isLiked)
    }

    fun findById(id: Long): Optional<Post> = postRepository.findById(id)

    /*@Transactional
    public Post modify(Long postId, String email, String title, String content, Long categoryId) {

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        // api 요청자 actor 찾기
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(actor.getId())) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        // 변경하려고 하는 카테고리 조회
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다."));

        // 변경하려고 하는 카테고리가 현재 게시글의 게시판에 속하는지
        if (!category.getBoardId().equals(post.getBoard().getId())) {
            throw new IllegalArgumentException("해당 게시판에서 사용할 수 없는 카테고리입니다.");
        }

        post.update(HtmlStyles.title, content, category);

        return post;

    }*/

    /*@Transactional
    public void delete(Long postId, String email) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시물을 찾을 수 없습니다."));

        // 요청 유저 찾기
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (post.isDeleted()) {
            throw new IllegalArgumentException("이미 삭제된 게시물입니다.");
        }

        if (!post.getAuthor().getId().equals(actor.getId())) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }

        post.delete();

        evictTop5Cache(post.getBoard().getId());
    }*/

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

    /*public List<PostResponseDto> getTop5Posts(Long boardId) {
        String cacheKey = "top5:board:" + boardId;

        // 1. 캐시에서 JSON 문자열 조회
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);

        try {
            if (cachedJson != null) {
                // JSON 문자열을 List<PostResponseDto>로 역직렬화
                return objectMapper.readValue(cachedJson, new TypeReference<List<PostResponseDto>>() {});
            }

            // 캐시 부재 시 DB 조회
            List<Post> posts = postRepository.findTop5ByBoardId(boardId, PageRequest.of(0, 5));
            List<PostResponseDto> response = posts.stream().map(PostResponseDto::new).collect(Collectors.toList());

            // 객체를 JSON 문자열로 직렬화하여 저장
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));

            return response;

        } catch (JsonProcessingException e) {
            // 직렬화 실패 시 로그 기록 후 DB 결과 반환 (캐시 없이 반환)
            return postRepository.findTop5ByBoardId(boardId, PageRequest.of(0, 5))
                    .stream().map(PostResponseDto::new).collect(Collectors.toList());
        }
    }*/

    /*// redis 캐시 삭제 (글 등록, 글 삭제, likeCount 변경 메서드에서 사용됨)
    private void evictTop5Cache(Long boardId) {
        String cacheKey = "top5:board:" + boardId;
        redisTemplate.delete(cacheKey);
        log.info("캐시 무효화 완료: {}", cacheKey);
    }*/

    companion object {
        private const val PAGE_SIZE = 20
    }
}
