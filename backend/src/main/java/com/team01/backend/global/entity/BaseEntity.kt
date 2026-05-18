package com.team01.backend.global.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
open class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(name = "createdAt", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @Column(name = "modifiedAt", nullable = false)
    lateinit var modifiedAt: LocalDateTime
        protected set

    @PrePersist
    protected fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        modifiedAt = now
    }

    @PreUpdate
    protected fun preUpdate() {
        modifiedAt = LocalDateTime.now()
    }
}
