package com.team01.backend.domain.category.entity

import com.team01.backend.global.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import lombok.Getter

@Entity
@Getter
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["boardId", "name"])])
class Category(boardId: Long, name: String) : BaseEntity() {
    var boardId: Long = boardId
        protected set

    var name: String = name
        protected set

    fun update(name: String) {
        this.name = name
    }
}