package com.team01.backend.domain.notification.event

//@Getter
data class ReplyCreatedEvent(
    val postId: Long,              // post Id
    val commentId: Long,           // comment Id
    val commentWriterId: Long,     // 댓글 작성자 userId
    val replyId: Long,             // 답글 Id
    val replyWriterId: Long,       // 답글 작성자 userId
    val replyContent: String       // 알림 내용 + 답글 내용
)