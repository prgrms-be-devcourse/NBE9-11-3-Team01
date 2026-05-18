package com.team01.backend.domain.notification.repository

import com.team01.backend.domain.notification.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByReceiverIdOrderByCreatedAtDesc(receiverId: Long?):List<Notification>
}
