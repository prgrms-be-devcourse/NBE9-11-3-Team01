package com.team01.backend.domain.user.repository

import com.team01.backend.domain.user.entity.Role
import com.team01.backend.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmailAndRoleNot(email: String, role: Role): Optional<User>
    fun findByNicknameAndRoleNot(nickname: String, role: Role): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}
