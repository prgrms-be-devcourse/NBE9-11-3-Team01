package com.team01.backend.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.team01.backend.global.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {  // 인가 실패 관리

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        ex: AccessDeniedException
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json;charset=UTF-8"

        val apiResponse = ApiResponse.ofFailure<Void>("FORBIDDEN", "권한이 없습니다.")

        response.writer.write(
            objectMapper.writeValueAsString(apiResponse)
        )
    }
}