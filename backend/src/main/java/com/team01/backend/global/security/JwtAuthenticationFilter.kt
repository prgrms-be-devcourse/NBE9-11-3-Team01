package com.team01.backend.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

/**
 * 모든 API 요청이 들어올 때마다 JWT 토큰의 유효성을 검사하는 필터입니다.
 *
 * 기존 자바 코드의 로직을 계승하며, 코틀린의 확장 함수를 사용하여 가독성을 높였습니다.
 */
class JwtAuthenticationFilter(private val jwtTokenProvider: JwtTokenProvider) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1. [수정] HTTP 요청의 쿠키 저장소에서 accessToken을 추출합니다.
        //
        val token = resolveTokenFromCookie(request)

        // 2. 토큰이 존재하고 유효성 검증을 통과한다면 인증 정보를 설정합니다.
        //
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 토큰으로부터 사용자의 인증 정보(Authentication)를 가져옵니다.
            val authentication = jwtTokenProvider.getAuthentication(token)
            // SecurityContext에 인증 정보를 저장하여, 이후 로직에서 인증된 사용자로 인식하게 합니다.
            SecurityContextHolder.getContext().authentication = authentication
        }

        // 3. 다음 필터 단계로 요청을 넘깁니다.
        //
        filterChain.doFilter(request, response)
    }

    /**
     * [신규 추가] 요청에 포함된 쿠키 배열을 탐색하여 'accessToken' 값을 찾아내는 메서드입니다.
     *
     * 코틀린의 find 함수를 사용하여 자바의 for문보다 간결하게 구현했습니다.
     */
    private fun resolveTokenFromCookie(request: HttpServletRequest): String? {
        // request.cookies가 null일 수 있으므로 세이프 콜(?.)을 사용합니다.
        return request.cookies?.find { it.name == "accessToken" }?.value
    }
}