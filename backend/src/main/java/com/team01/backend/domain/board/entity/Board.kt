package com.team01.backend.domain.board.entity

import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import lombok.Getter

@Entity
@Getter
@Table(name = "boards")
class Board : BaseEntity {
    @Column(nullable = false, length = 50)
    var name: String=""

    @Column(length = 200)
    var description: String=""

    var isDeleted: Boolean = false

    constructor(name: String, description: String){
        this.name = name
        this.description = description
    }

    fun update(name: String, description: String) { // 게시판 수정
        this.name = name
        this.description = description
    }
}
