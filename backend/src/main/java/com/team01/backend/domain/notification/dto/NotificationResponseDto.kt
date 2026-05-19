package com.team01.backend.domain.notification.dto

import com.team01.backend.domain.notification.entity.Notification
import java.time.LocalDateTime

data class NotificationResponseDto(
    val id: Long,
    val receiverId: Long,  //받는 사람 (userId)
    val senderId: Long,  //보내는 사람 (userId)
    val targetId: Long,  //url
    val content: String,  // 알림 내용
    val alreadyRead: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: Notification): NotificationResponseDto =
            NotificationResponseDto(
                id = notification.id,
                receiverId = notification.receiverId,
                senderId = notification.senderId,
                targetId = notification.targetId,
                content = notification.content,
                alreadyRead = notification.alreadyRead,
                createdAt = notification.createdAt,
            )
    }
}
