package com.team01.backend.domain.post.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.team01.backend.domain.board.entity.Board
import com.team01.backend.domain.board.repository.BoardRepository
import com.team01.backend.domain.category.entity.Category
import com.team01.backend.domain.category.repository.CategoryRepository
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.entity.PostLike
import com.team01.backend.domain.post.repository.PostLikeRepository
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostServiceTest {

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var commentService: CommentService

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var boardRepository: BoardRepository

    @Mock
    lateinit var categoryRepository: CategoryRepository

    @Mock
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var postLikeRepository: PostLikeRepository

    @InjectMocks
    lateinit var postService: PostService

    @Test
    @DisplayName("getPostById - 작성자 본인이 좋아요한 게시글 정상 조회")
    fun t1() {
        // given
        val board = board(id = 1L, name = "자유게시판")
        val category = category(id = 10L, boardId = 1L, name = "일반")
        val author = user(id = 100L, email = "author@test.com", nickname = "작성자")
        val post = post(id = 1000L, author = author, board = board, category = category, title = "제목", content = "내용")
        val postLike = PostLike(author, post).apply { setBaseFields(id = 5000L) }

        whenever(postRepository.findWithDetailsById(1000L)).thenReturn(post)
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))
        whenever(postLikeRepository.findByUserIdAndPostId(100L, 1000L)).thenReturn(postLike)
        whenever(commentService.getCommentsByPostId(1000L, "author@test.com")).thenReturn(emptyList())

        // when
        val result = postService.getPostById(1000L, "author@test.com")

        // then
        assertEquals(1000L, result.id)
        assertEquals(1L, result.boardId)
        assertEquals(10L, result.categoryId)
        assertEquals("제목", result.title)
        assertEquals("내용", result.content)
        assertEquals("작성자", result.author)
        assertTrue(result.owner)
        assertTrue(result.liked)
        assertTrue(result.comments.isEmpty())
        verify(postRepository).findWithDetailsById(1000L)
        verify(postLikeRepository).findByUserIdAndPostId(100L, 1000L)
        verify(commentService).getCommentsByPostId(1000L, "author@test.com")
    }

    @Test
    @DisplayName("getPostById - 작성자 아닌 사용자가 좋아요하지 않은 게시글 정상 조회")
    fun t2() {
        // given
        val board = board(id = 1L)
        val category = category(id = 10L, boardId = 1L)
        val author = user(id = 100L, email = "author@test.com", nickname = "작성자")
        val viewer = user(id = 200L, email = "viewer@test.com", nickname = "조회자")
        val post = post(id = 1000L, author = author, board = board, category = category)

        whenever(postRepository.findWithDetailsById(1000L)).thenReturn(post)
        whenever(userRepository.findByEmail("viewer@test.com")).thenReturn(Optional.of(viewer))
        whenever(postLikeRepository.findByUserIdAndPostId(200L, 1000L)).thenReturn(null)
        whenever(commentService.getCommentsByPostId(1000L, "viewer@test.com")).thenReturn(emptyList())

        // when
        val result = postService.getPostById(1000L, "viewer@test.com")

        // then
        assertFalse(result.owner)
        assertFalse(result.liked)
        verify(postLikeRepository).findByUserIdAndPostId(200L, 1000L)
    }

    @Test
    @DisplayName("getPostById - 게시글이 없으면 EntityNotFoundException")
    fun t3() {
        // given
        whenever(postRepository.findWithDetailsById(1000L)).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.getPostById(1000L, "user@test.com")
        }
        verifyNoInteractions(userRepository, postLikeRepository, commentService)
    }

    @Test
    @DisplayName("getPostById - 게시글이 soft delete되면 EntityNotFoundException")
    fun t4() {
        // given
        val post = post().apply { delete() }
        whenever(postRepository.findWithDetailsById(1000L)).thenReturn(post)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.getPostById(1000L, "user@test.com")
        }
        verifyNoInteractions(userRepository, postLikeRepository, commentService)
    }

    @Test
    @DisplayName("getPostById - 게시판이 soft delete되면 EntityNotFoundException")
    fun t5() {
        // given
        val board = board().apply { delete() }
        val post = post(board = board)
        whenever(postRepository.findWithDetailsById(1000L)).thenReturn(post)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.getPostById(1000L, "user@test.com")
        }
        verifyNoInteractions(userRepository, postLikeRepository, commentService)
    }

    @Test
    @DisplayName("write - 정상 게시글 작성 시 저장 및 캐시 무효화")
    fun t6() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val board = board(id = 1L)
        val category = category(id = 10L, boardId = 1L)
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))
        whenever(boardRepository.findByIdAndDeletedFalse(1L)).thenReturn(board)
        whenever(categoryRepository.findById(10L)).thenReturn(Optional.of(category))
        whenever(categoryRepository.getReferenceById(10L)).thenReturn(category)
        whenever(postRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<Post>(0).apply { setBaseFields(id = 1000L) }
        }

        // when
        val result = postService.write("author@test.com", "새 제목", "새 내용", 1L, 10L)

        // then
        val postCaptor = argumentCaptor<Post>()
        verify(postRepository).save(postCaptor.capture())
        assertEquals("새 제목", postCaptor.firstValue.title)
        assertEquals("새 내용", postCaptor.firstValue.content)
        assertSame(author, postCaptor.firstValue.author)
        assertSame(board, postCaptor.firstValue.board)
        assertSame(category, postCaptor.firstValue.category)
        assertSame(postCaptor.firstValue, result)
        verify(redisTemplate).delete("top5:board:1")
    }

    @Test
    @DisplayName("write - 사용자가 없으면 EntityNotFoundException")
    fun t7() {
        // given
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.write("missing@test.com", "제목", "내용", 1L, 10L)
        }
        verifyNoInteractions(boardRepository, categoryRepository, postRepository, redisTemplate)
    }

    @Test
    @DisplayName("write - 게시판이 없거나 soft delete되면 EntityNotFoundException")
    fun t8() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))
        whenever(boardRepository.findByIdAndDeletedFalse(1L)).thenReturn(null)

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.write("author@test.com", "제목", "내용", 1L, 10L)
        }
        verifyNoInteractions(categoryRepository, postRepository, redisTemplate)
    }

    @Test
    @DisplayName("write - 카테고리가 해당 게시판 소속이 아니면 IllegalArgumentException")
    fun t9() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val board = board(id = 1L)
        val category = category(id = 10L, boardId = 2L)
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))
        whenever(boardRepository.findByIdAndDeletedFalse(1L)).thenReturn(board)
        whenever(categoryRepository.findById(10L)).thenReturn(Optional.of(category))

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            postService.write("author@test.com", "제목", "내용", 1L, 10L)
        }
        verify(categoryRepository, never()).getReferenceById(any())
        verifyNoInteractions(postRepository, redisTemplate)
    }

    @Test
    @DisplayName("modify - 정상 수정")
    fun t10() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val board = board(id = 1L)
        val oldCategory = category(id = 10L, boardId = 1L, name = "기존")
        val newCategory = category(id = 11L, boardId = 1L, name = "변경")
        val post = post(id = 1000L, author = author, board = board, category = oldCategory)
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))
        whenever(categoryRepository.findById(11L)).thenReturn(Optional.of(newCategory))

        // when
        val result = postService.modify(1000L, "author@test.com", "수정 제목", "수정 내용", 11L)

        // then
        assertSame(post, result)
        assertEquals("수정 제목", post.title)
        assertEquals("수정 내용", post.content)
        assertSame(newCategory, post.category)
    }

    @Test
    @DisplayName("modify - 게시글이 없으면 EntityNotFoundException")
    fun t11() {
        // given
        whenever(postRepository.findById(1000L)).thenReturn(Optional.empty())

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.modify(1000L, "author@test.com", "수정 제목", "수정 내용", 10L)
        }
        verifyNoInteractions(userRepository, categoryRepository)
    }

    @Test
    @DisplayName("modify - 사용자가 없으면 EntityNotFoundException")
    fun t12() {
        // given
        val post = post(id = 1000L)
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.modify(1000L, "missing@test.com", "수정 제목", "수정 내용", 10L)
        }
        verifyNoInteractions(categoryRepository)
    }

    @Test
    @DisplayName("modify - 작성자가 아닌 사용자가 수정하면 AccessDeniedException")
    fun t13() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val otherUser = user(id = 200L, email = "other@test.com")
        val post = post(id = 1000L, author = author)
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser))

        // when & then
        assertThrows(AccessDeniedException::class.java) {
            postService.modify(1000L, "other@test.com", "수정 제목", "수정 내용", 10L)
        }
        verifyNoInteractions(categoryRepository)
    }

    @Test
    @DisplayName("modify - 카테고리가 해당 게시판 소속이 아니면 IllegalArgumentException")
    fun t14() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val board = board(id = 1L)
        val post = post(id = 1000L, author = author, board = board)
        val otherBoardCategory = category(id = 10L, boardId = 2L)
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))
        whenever(categoryRepository.findById(10L)).thenReturn(Optional.of(otherBoardCategory))

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            postService.modify(1000L, "author@test.com", "수정 제목", "수정 내용", 10L)
        }
    }

    @Test
    @DisplayName("delete - 정상 삭제 시 soft delete 및 캐시 무효화")
    fun t15() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val board = board(id = 1L)
        val post = post(id = 1000L, author = author, board = board)
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))

        // when
        postService.delete(1000L, "author@test.com")

        // then
        assertTrue(post.deleted)
        verify(redisTemplate).delete("top5:board:1")
    }

    @Test
    @DisplayName("delete - 게시글이 없으면 EntityNotFoundException")
    fun t16() {
        // given
        whenever(postRepository.findById(1000L)).thenReturn(Optional.empty())

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            postService.delete(1000L, "author@test.com")
        }
        verifyNoInteractions(userRepository, redisTemplate)
    }

    @Test
    @DisplayName("delete - 이미 삭제된 게시글이면 IllegalArgumentException")
    fun t17() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val post = post(id = 1000L, author = author).apply { delete() }
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author))

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            postService.delete(1000L, "author@test.com")
        }
        verify(redisTemplate, never()).delete(any<String>())
    }

    @Test
    @DisplayName("delete - 작성자가 아닌 사용자가 삭제하면 AccessDeniedException")
    fun t18() {
        // given
        val author = user(id = 100L, email = "author@test.com")
        val otherUser = user(id = 200L, email = "other@test.com")
        val post = post(id = 1000L, author = author)
        whenever(postRepository.findById(1000L)).thenReturn(Optional.of(post))
        whenever(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser))

        // when & then
        assertThrows(AccessDeniedException::class.java) {
            postService.delete(1000L, "other@test.com")
        }
        assertFalse(post.deleted)
        verify(redisTemplate, never()).delete(any<String>())
    }

    @Test
    @DisplayName("getPostsByBoardId - 키워드/카테고리 필터 포함 정상 페이징 조회")
    fun t19() {
        // given
        val board = board(id = 1L)
        val category = category(id = 10L, boardId = 1L)
        val post = post(id = 1000L, board = board, category = category, title = "검색 제목")
        whenever(postRepository.searchByBoardId(eq(1L), eq("검색"), eq(10L), any<Pageable>(), eq("latest")))
            .thenReturn(PageImpl(listOf(post), PageRequest.of(0, 20), 1))

        // when
        val result = postService.getPostsByBoardId(1L, 1, "검색", 10L, "latest")

        // then
        assertEquals(1, result.currentPage)
        assertEquals(1, result.totalPages)
        assertEquals(1L, result.totalElements)
        assertFalse(result.hasNext)
        assertEquals(1, result.posts.size)
        assertEquals(1000L, result.posts[0].id)
        assertEquals("검색 제목", result.posts[0].title)
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(postRepository).searchByBoardId(eq(1L), eq("검색"), eq(10L), pageableCaptor.capture(), eq("latest"))
        assertEquals(0, pageableCaptor.firstValue.pageNumber)
        assertEquals(20, pageableCaptor.firstValue.pageSize)
    }

    @Test
    @DisplayName("getPostsByBoardAndCategory - 정상 조회")
    fun t20() {
        // given
        val board = board(id = 1L)
        val category = category(id = 10L, boardId = 1L)
        val post = post(id = 1000L, board = board, category = category)
        whenever(categoryRepository.findById(10L)).thenReturn(Optional.of(category))
        whenever(postRepository.searchByBoardId(eq(1L), isNull(), eq(10L), any<Pageable>(), eq("latest")))
            .thenReturn(PageImpl(listOf(post), PageRequest.of(0, 20), 1))

        // when
        val result = postService.getPostsByBoardAndCategory(1L, 10L, 1, null, "latest")

        // then
        assertEquals(1, result.posts.size)
        assertEquals(10L, result.posts[0].categoryId)
        verify(postRepository).searchByBoardId(eq(1L), isNull(), eq(10L), any<Pageable>(), eq("latest"))
    }

    @Test
    @DisplayName("getPostsByBoardAndCategory - 카테고리가 해당 게시판 소속이 아니면 IllegalArgumentException")
    fun t21() {
        // given
        val category = category(id = 10L, boardId = 2L)
        whenever(categoryRepository.findById(10L)).thenReturn(Optional.of(category))

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            postService.getPostsByBoardAndCategory(1L, 10L, 1, null, "latest")
        }
        verify(postRepository, never()).searchByBoardId(any(), any(), any(), any(), any())
    }

    private fun user(
        id: Long = 100L,
        email: String = "user@test.com",
        nickname: String = "사용자",
    ): User = User(
        email = email,
        password = "password",
        nickname = nickname,
    ).apply { setBaseFields(id = id) }

    private fun board(
        id: Long = 1L,
        name: String = "게시판",
        description: String = "게시판 설명",
    ): Board = Board(name, description).apply { setBaseFields(id = id) }

    private fun category(
        id: Long = 10L,
        boardId: Long = 1L,
        name: String = "카테고리",
    ): Category = Category(boardId, name).apply { setBaseFields(id = id) }

    private fun post(
        id: Long = 1000L,
        author: User = user(),
        board: Board = board(),
        category: Category = category(boardId = board.id),
        title: String = "제목",
        content: String = "내용",
    ): Post = Post(
        author = author,
        title = title,
        content = content,
        board = board,
        category = category,
    ).apply { setBaseFields(id = id) }

    private fun BaseEntity.setBaseFields(
        id: Long,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 5, 18, 12, 0),
        modifiedAt: LocalDateTime = createdAt,
    ) {
        setField("id", id)
        setField("createdAt", createdAt)
        setField("modifiedAt", modifiedAt)
    }

    private fun BaseEntity.setField(name: String, value: Any) {
        val field = BaseEntity::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }
}