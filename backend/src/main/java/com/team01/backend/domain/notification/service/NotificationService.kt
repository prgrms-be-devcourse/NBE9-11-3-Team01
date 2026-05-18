package com.team01.backend.domain.notification.service

import com.team01.backend.domain.notification.dto.NotificationReadResponseDto
import com.team01.backend.domain.notification.dto.NotificationResponseDto
import com.team01.backend.domain.notification.entity.Notification
import com.team01.backend.domain.notification.event.CommentCreatedEvent
import com.team01.backend.domain.notification.event.ReplyCreatedEvent
import com.team01.backend.domain.notification.repository.NotificationRepository
import com.team01.backend.domain.notification.repository.SseEmitterRepository
import com.team01.backend.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import kotlin.jvm.optionals.getOrNull

@Service
class NotificationService( // 알림을 실제로 보내는 역할, 다시 볼 수 있도록 DB에 저장하는 역할
    private val notificationRepository: NotificationRepository,
    private val sseEmitterRepository: SseEmitterRepository,
    private val userRepository: UserRepository
) {
    @Async
    @EventListener
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
}