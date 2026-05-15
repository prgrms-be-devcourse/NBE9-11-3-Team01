package com.team01.backend.domain.user.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

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
) {
    @CreatedDate
    @Column(updatable = false, nullable = false)
    var createdAt: LocalDateTime? = null

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedAt: LocalDateTime? = null

    @PrePersist
    fun prePersist() {
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
