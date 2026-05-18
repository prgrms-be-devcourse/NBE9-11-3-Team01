package com.team01.backend.domain.notification.repository

import org.springframework.stereotype.Repository
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Repository
class SseEmitterRepository {
    private val emitters = ConcurrentHashMap<Long, MutableList<SseEmitter>>()

    fun save(userId: Long, emitter: SseEmitter) {
        emitters.getOrPut(userId){CopyOnWriteArrayList()}
            .add(emitter)
    }

    fun findByUserId(userId: Long): List<SseEmitter> {
        return emitters[userId]?: emptyList()
    }

    fun delete(userId: Long, emitter: SseEmitter) {
        emitters[userId]?.remove(emitter)
    }
}