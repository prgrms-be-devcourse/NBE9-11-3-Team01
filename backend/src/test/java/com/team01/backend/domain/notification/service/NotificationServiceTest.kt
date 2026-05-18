package com.team01.backend.domain.notification.service

import com.team01.backend.domain.comment.dto.CommentRequestDto
import com.team01.backend.domain.comment.service.CommentService
import com.team01.backend.domain.notification.entity.Notification
import com.team01.backend.domain.notification.repository.NotificationRepository
import com.team01.backend.domain.notification.repository.SseEmitterRepository
import com.team01.backend.domain.post.service.PostService
import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.Commit
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.List

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
internal class NotificationServiceTest {
    @Autowired
    private val commentService: CommentService? = null

    @Autowired
    private val notificationRepository: NotificationRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val postService: PostService? = null

    @MockitoBean
    private val emitterRepository: SseEmitterRepository? = null

    @Test
    @Commit
    fun shouldNotifyPostOwnerWhenOtherUserComments() {
        val commenter = userRepository!!.save<User>(
            User("commenter@test.com", "pw", "commenter", null, Role.USER)
        )

        val post = postService!!.write("user1@test.com", "title", "content", 1L, 2L) //user1

        val emitter = Mockito.mock<SseEmitter>(SseEmitter::class.java)

        Mockito.`when`<MutableList<SseEmitter>>(emitterRepository!!.findByUserId(1L))
            .thenReturn(List.of<SseEmitter>(emitter))

        commentService!!.writeComment(1L, CommentRequestDto("comment1", null), "commenter@test.com")

        // then
        val notifications: MutableList<Notification?> =
            notificationRepository!!.findByReceiverIdOrderByCreatedAtDesc(1L)

        Assertions.assertThat<Notification?>(notifications).hasSize(1)
        Assertions.assertThat(notifications.get(0)!!.receiverId)
            .isEqualTo(1L)
    }
}