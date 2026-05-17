package com.team01.backend.domain.notification.controller

import com.team01.backend.domain.notification.repository.SseEmitterRepository
import com.team01.backend.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

@Tag(name = "실시간 알림", description = "실시간 알림 연결 관련 API")
@RestController
class SseController(
    private val sseEmitterRepository: SseEmitterRepository,
    private val userService: UserService
) {

    @Operation(summary = "실시간 알림 연결", description = "실시간 알림을 보내기 위해 클라이언트와 최초 연결합니다. 로그인한 상태에서만 가능합니다.")
    @GetMapping("/subscribe")
    fun subscribe(@AuthenticationPrincipal user: UserDetails): SseEmitter {

        // UserDetail에서 userId 가져오기
        val username = user.username
        val userId = userService.findIdByUsername(username)

        // 클라이언트와 연결 유지 객체(emitter) 생성 (1시간)
        val emitter = SseEmitter(60 * 60 * 1000L)

        sseEmitterRepository.save(userId, emitter)

        // 람다식 축약 적용하여 간결하게 콜백 등록
        emitter.onCompletion { sseEmitterRepository.delete(userId, emitter) }
        emitter.onTimeout { sseEmitterRepository.delete(userId, emitter) }
        emitter.onError { sseEmitterRepository.delete(userId, emitter) }

        // SSE 연결 알림 보내기 (연결 확인용)
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connect")
                    .data("connected")
            )
        } catch (e: IOException) {
            emitter.complete()
        }

        return emitter
    }
}