package com.team01.backend.domain.notification.controller

import com.team01.backend.domain.notification.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.nio.file.AccessDeniedException

@Tag(name = "실시간 알림", description = "실시간 알림 연결 관련 API")
@RestController
class SseController(
    private val notificationService: NotificationService
){
    @Operation(summary = "실시간 알림 연결", description = "실시간 알림을 보내기 위해 클라이언트와 최초 연결합니다. 로그인한 상태에서만 가능합니다.")
    @GetMapping("/subscribe")
    fun subscribe(@AuthenticationPrincipal user: UserDetails?): SseEmitter {
        val validUser = user?: throw AccessDeniedException("로그인이 필요한 서비스입니다.")
        return notificationService.subscribe(validUser)
    }
}