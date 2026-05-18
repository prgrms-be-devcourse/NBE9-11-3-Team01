package com.team01.backend.domain.notification.event

data class CommentCreatedEvent(
    val postId: Long,              // post Id
    val postOwnerId: Long,         // post author userId
    val commentId: Long,           // comment Id
    val commentWriterId: Long,     // 댓글 작성자 userId
    val commentContent: String     // 알림 내용 + 댓글 내용
)