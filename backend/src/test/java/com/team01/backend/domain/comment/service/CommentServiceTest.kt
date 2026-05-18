package com.team01.backend.domain.comment.service

//import com.team01.backend.config.TestMailConfig
import com.team01.backend.config.TestRedisConfig
import com.team01.backend.domain.comment.dto.CommentDeleteResponseDto
import com.team01.backend.domain.comment.dto.CommentRequestDto
import com.team01.backend.domain.comment.repository.CommentLikeRepository
import com.team01.backend.domain.comment.repository.CommentRepository
import com.team01.backend.domain.post.entity.Post
import com.team01.backend.domain.post.repository.PostRepository
import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
//@Import(TestMailConfig::class, TestRedisConfig::class)
@Transactional
class CommentServiceTest {

    @Autowired private lateinit var commentService: CommentService
    @Autowired private lateinit var commentRepository: CommentRepository
    @Autowired private lateinit var commentLikeRepository: CommentLikeRepository
    @Autowired private lateinit var postRepository: PostRepository
    @Autowired private lateinit var userRepository: UserRepository

    private lateinit var testUser: User
    private lateinit var testPost: Post

    @BeforeEach
    fun setUp() {
        testUser = userRepository.findByEmail("user1@test.com").orElseThrow()
        testPost = postRepository.findById(1L).orElseThrow()
    }

    @Test
    @DisplayName("댓글 목록 조회 - 없으면 빈 목록")
    fun getCommentsByPostId_empty() {
        assertTrue(commentService.getCommentsByPostId(99999L, testUser.email).isEmpty())
    }

    @Test
    @DisplayName("댓글 작성 후 목록 조회")
    fun getCommentsByPostId_afterWrite() {
        commentService.writeComment(testPost.id!!, CommentRequestDto("서비스 댓글", null), testUser.email)
        val list = commentService.getCommentsByPostId(testPost.id!!, testUser.email)
        assertTrue(list.any { it.content == "서비스 댓글" })
    }

    @Test
    @DisplayName("댓글 수정 실패 - 본인이 아님")
    fun updateComment_notOwner_fails() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("대상", null), testUser.email)
        val other = userRepository.save(User("svc-other@test.com", "pw", "타인", null, Role.USER, null))
        assertThrows(IllegalArgumentException::class.java) {
            commentService.updateComment(dto.id, CommentRequestDto("해킹", null), other.email)
        }
    }

    @Test
    @DisplayName("댓글 수정 실패 - 삭제된 댓글")
    fun updateComment_deleted_fails() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("삭제대상", null), testUser.email)
        commentService.deleteComment(dto.id, testUser.email)
        assertThrows(IllegalArgumentException::class.java) {
            commentService.updateComment(dto.id, CommentRequestDto("수정", null), testUser.email)
        }
    }

    @Test
    @DisplayName("댓글 삭제 - 플레이스홀더 반환")
    fun deleteComment_success() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("삭제", null), testUser.email)
        val result = commentService.deleteComment(dto.id, testUser.email)
        assertEquals(dto.id, result.id)
        assertEquals(CommentDeleteResponseDto.DELETED_CONTENT_PLACEHOLDER, result.message)
        assertTrue(commentRepository.findById(dto.id).orElseThrow().deleted)
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인이 아님")
    fun deleteComment_notOwner_fails() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("권한", null), testUser.email)
        val other = userRepository.save(User("svc-deny@test.com", "pw", "거부", null, Role.USER, null))
        assertThrows(AccessDeniedException::class.java) {
            commentService.deleteComment(dto.id, other.email)
        }
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 등록 및 취소")
    fun toggleCommentLike_likeAndUnlike() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("좋아요", null), testUser.email)
        val liked = commentService.toggleCommentLike(dto.id, testUser.email)
        assertEquals(1, liked.likeCount)
        assertTrue(liked.liked)
        val unliked = commentService.toggleCommentLike(dto.id, testUser.email)
        assertEquals(0, unliked.likeCount)
        assertTrue(!unliked.liked)
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 삭제된 댓글")
    fun toggleCommentLike_deletedComment_fails() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("삭제후", null), testUser.email)
        commentService.deleteComment(dto.id, testUser.email)
        assertThrows(IllegalArgumentException::class.java) {
            commentService.toggleCommentLike(dto.id, testUser.email)
        }
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 삭제된 게시글")
    fun toggleCommentLike_deletedPost_fails() {
        val dto = commentService.writeComment(testPost.id!!, CommentRequestDto("글삭제", null), testUser.email)
        ReflectionTestUtils.setField(testPost, "deleted", true)
        postRepository.saveAndFlush(testPost)
        assertThrows(EntityNotFoundException::class.java) {
            commentService.toggleCommentLike(dto.id, testUser.email)
        }
    }

    @Test
    @DisplayName("대댓글 작성 후 트리 조회")
    fun writeReply_andGetComments() {
        val parent = commentService.writeComment(testPost.id!!, CommentRequestDto("부모", null), testUser.email)
        commentService.writeComment(testPost.id!!, CommentRequestDto("자식", parent.id), testUser.email)
        val root = commentService.getCommentsByPostId(testPost.id!!, testUser.email).first { it.id == parent.id }
        assertEquals(1, root.replies.size)
        assertEquals("자식", root.replies[0].content)
    }
}
