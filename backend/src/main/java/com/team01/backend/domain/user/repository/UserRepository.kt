package com.team01.backend.domain.user.repository

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    // 탈퇴하지 않은 사용자 조회 (기존의 유연한 비즈니스 조건 유지)
    fun findByEmailAndRoleNot(email: String, role: Role): User?
    fun findByNicknameAndRoleNot(nickname: String, role: Role): User?

    // 기본 단일 식별자 조회 (Optional 제거 후 Nullable 타입으로 개편)
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}