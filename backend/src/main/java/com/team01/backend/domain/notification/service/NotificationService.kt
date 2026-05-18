package com.team01.backend.domain.notification.service

import com.team01.backend.domain.notification.dto.NotificationReadResponseDto
import com.team01.backend.domain.notification.dto.NotificationResponseDto
import com.team01.backend.domain.notification.entity.Notification
import com.team01.backend.domain.notification.event.CommentCreatedEvent
import com.team01.backend.domain.notification.event.ReplyCreatedEvent
import com.team01.backend.domain.notification.repository.NotificationRepository
import com.team01.backend.domain.notification.repository.SseEmitterRepository
import com.team01.backend.domain.user.repository.UserRepository
import com.team01.backend.domain.user.service.UserService
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import kotlin.jvm.optionals.getOrNull

@Service
class NotificationService( // 알림을 실제로 보내는 역할, 다시 볼 수 있도록 DB에 저장하는 역할
    private val notificationRepository: NotificationRepository,
    private val sseEmitterRepository: SseEmitterRepository,
    private val userRepository: UserRepository,
    private val userService: UserService
) {
    @Async
    @EventListener
    @Transactional
    fun handleNotification(event: CommentCreatedEvent) {
        // 1. 실제 DB 저장
        val notification = notificationRepository.save(
            Notification(event.postOwnerId, event.commentWriterId, event.postId, "댓글이 달렸습니다.${event.commentContent}") //targetId를 postId로 저장
        )

        // 2. SSE 전송
        val emitters = sseEmitterRepository.findByUserId(event.postOwnerId)

        for(emitter in emitters) {
            try{
                emitter.send(notification)
            } catch (e: IOException) {
                emitter.complete()
            }
        }
    }

    @Async
    @EventListener
    @Transactional
    fun handleNotification(event: ReplyCreatedEvent) {
        // 1. 실제 DB 저장
        val notification = notificationRepository.save(
            Notification(event.commentWriterId, event.replyWriterId, event.postId, "답글이 달렸습니다.${event.replyContent}")
        )

        // 2. SSE 전송
        val emitters = sseEmitterRepository.findByUserId(event.commentWriterId)

        for(emitter in emitters) {
            try{
                emitter.send(notification)
            } catch (e: IOException) {
                emitter.complete()
            }
        }
    }

    @Transactional(readOnly = true)
    fun getAllNotification(email: String): List<NotificationResponseDto> {
        val user = userRepository.findByEmail(email).getOrNull()
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(user.id)
            .map {NotificationResponseDto.from(it)}
    }

    @Transactional
    fun read(notificationId: Long, email: String): NotificationReadResponseDto {
        val user = userRepository.findByEmail(email).getOrNull()
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        val notification = notificationRepository.findById(notificationId)
            .orElseThrow {EntityNotFoundException("알림을 찾을 수 없습니다.")}

        // 권한 검증 (== 비교 사용)
        if (notification.receiverId != user.id) {
            throw AccessDeniedException("권한이 없습니다")
        }

        notification.read()

        return NotificationReadResponseDto.from(notification)
    }

    // SSE 연결
    fun subscribe(@AuthenticationPrincipal user: UserDetails): SseEmitter {

        // UserDetail에서 userId 가져오기
        val username = user.username
        val userId = userService.findIdByUsername(username)

        // 클라이언트와 연결 유지 객체(emitter) 생성 (1시간)
        val emitter = SseEmitter(60 * 60 * 1000L)

        sseEmitterRepository.save(userId, emitter)

        // 람다식 축약 적용하여 간결하게 콜백 등록
        emitter.onCompletion { sseEmitterRepository.delete(userId, emitter) }
        emitter.onTimeout { sseEmitterRepository.delete(userId, emitter) }
        emitter.onError { sseEmitterRepository.delete(userId, emitter) }

        // SSE 연결 알림 보내기 (연결 확인용)
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connect")
                    .data("connected")
            )
        } catch (e: IOException) {
            emitter.complete()
        }

        return emitter
    }
}