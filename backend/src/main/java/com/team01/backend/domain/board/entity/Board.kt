package com.team01.backend.domain.board.entity

import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "boards")
class Board(name: String, description: String) : BaseEntity() {
    @field:Column(nullable = false, length = 50)
    var name: String = name
    protected set

    @field:Column(length = 200)
    var description: String = description
    protected set

    @field:Column(name = "isDeleted")
    var deleted: Boolean = false
    protected set

    fun update(name: String, description: String) { // 게시판 수정
        this.name = name
        this.description = description
    }
    fun delete() {
        this.deleted = true
    }
}
