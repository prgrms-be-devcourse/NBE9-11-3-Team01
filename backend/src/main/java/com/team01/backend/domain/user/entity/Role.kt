package com.team01.backend.domain.user.entity

enum class Role(val key: String, val title: String) {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자"),
    WITHDRAWN("ROLE_WITHDRAWN", "회원탈퇴")
}
