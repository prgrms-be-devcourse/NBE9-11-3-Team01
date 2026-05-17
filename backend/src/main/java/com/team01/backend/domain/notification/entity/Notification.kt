package com.team01.backend.domain.notification.entity

import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.Entity
import lombok.Getter
import lombok.NoArgsConstructor

@Entity
@Getter
@NoArgsConstructor
class Notification(receiverId: Long, senderId: Long, targetId: Long, content: String) : BaseEntity() {
    var receiverId = receiverId
        protected set
    var senderId = senderId
        protected set
    var targetId = targetId
        protected set
    var content = content
        protected set
    var alreadyRead = false
        protected set
    fun read() {
        this.alreadyRead = true
    }
}
