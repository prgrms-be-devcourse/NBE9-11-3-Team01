package com.team01.backend.domain.user.entity

import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(unique = true, nullable = false)
    var nickname: String,

    @Column(columnDefinition = "TEXT")
    var profileImage: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Column
    var refreshToken: String? = null
) : BaseEntity() { // 순수 상속 구조 유지

    // BaseEntity의 prePersist()와 이름이 충돌하지 않도록 고유 명칭으로 변경
    @PrePersist
    fun prePersistUser() {
        if (profileImage.isNullOrBlank()) {
            profileImage = null
        }
    }

    fun updateInfo(nickname: String, password: String) {
        this.nickname = nickname
        this.password = password
    }

    fun updateRefreshToken(refreshToken: String?) {
        this.refreshToken = refreshToken
    }

    fun withdraw() {
        this.role = Role.WITHDRAWN
        this.refreshToken = null
    }

    fun updatePassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    fun updateProfileImage(profileImage: String?) {
        this.profileImage = profileImage
    }
}