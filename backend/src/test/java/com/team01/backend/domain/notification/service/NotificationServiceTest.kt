package com.team01.backend.domain.notification.service

import com.team01.backend.domain.comment.dto.CommentRequestDto
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.domain.notification.repository.NotificationRepository
import com.team01.backend.domain.notification.repository.SseEmitterRepository
import com.team01.backend.domain.post.service.PostService
import com.team01.backend.domain.user.service.AuthService
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
internal class NotificationServiceTest @Autowired constructor(
    private val commentService: CommentService,
    private val notificationRepository: NotificationRepository,
    private val postService: PostService,
    private val authService: AuthService,
    @MockitoBean
    private val emitterRepository: SseEmitterRepository
) {
    @BeforeEach
    fun signUp(){
        authService.signUp(
            email = "commenter1@test.com",
            password = "password1234",
            nickname = "commenter1",
        )
    }
    @AfterEach
    fun tearDown() {
        notificationRepository.deleteAllInBatch()
//        emitterRepository.delete()
    }

    @Test
    @DisplayName("댓글 달렸을 때 작성자에게 알림")
    @Transactional
    fun t1() {

        val post = postService.write("user1@test.com", "title", "content aaaaaaaa", 1L, 2L) //user1
        val emitter = mock(SseEmitter::class.java)
        given(emitterRepository.findByUserId(2L)).willReturn(listOf(emitter))

        // When
        val comment = commentService.writeComment(post.id, CommentRequestDto("comment1", null), "commenter1@test.com")

        // Then
        val notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(2L)

        Assertions.assertThat(notifications).hasSize(1)
        Assertions.assertThat(notifications[0].receiverId).isEqualTo(2L)
    }

    @Test
    @DisplayName("본인 글에 댓글 다는 경우 - 알림 X")
    @Transactional
    fun t2() {

        val post = postService.write("user1@test.com", "title", "content aaaaaaaa", 1L, 2L) //user1
        val emitter = mock(SseEmitter::class.java)
        given(emitterRepository.findByUserId(1L)).willReturn(listOf(emitter))
        // When
        val comment = commentService.writeComment(post.id, CommentRequestDto("comment1", null), "user1@test.com")

        // Then
        val notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L)

        Assertions.assertThat(notifications).hasSize(0)
    }
    @Test
    @DisplayName("답글 달렸을 때 댓글 작성자에게 알림")
    @Transactional
    fun t3() {
        val post = postService.write("user1@test.com", "title", "content aaaaaaaa", 1L, 2L) //user1
        val emitter = mock(SseEmitter::class.java)
        given(emitterRepository.findByUserId(3L)).willReturn(listOf(emitter))

        // When
        val comment = commentService.writeComment(post.id, CommentRequestDto("comment1", null), "user2@test.com")
        val reply = commentService.writeComment(post.id, CommentRequestDto("comment1", comment.id), "user3@test.com")

        // Then
        val notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(3L)

        Assertions.assertThat(notifications).hasSize(1)
        Assertions.assertThat(notifications[0].senderId).isEqualTo(4L)
    }
}