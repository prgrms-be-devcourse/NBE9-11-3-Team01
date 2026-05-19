package com.team01.backend.domain.notification.entity

import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.Entity

@Entity
class Notification(receiverId: Long, senderId: Long, targetId: Long, content: String) : BaseEntity() {
    var receiverId:Long = receiverId
        protected set
    var senderId:Long = senderId
        protected set
    var targetId:Long = targetId
        protected set
    var content:String = content
        protected set
    var alreadyRead: Boolean = false
        protected set
    fun read() {
        this.alreadyRead = true
    }
}
