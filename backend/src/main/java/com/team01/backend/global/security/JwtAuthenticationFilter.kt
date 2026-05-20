package com.team01.backend.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

/**
 * 모든 API 요청이 들어올 때마다 JWT 토큰의 유효성을 검사하는 필터입니다.
 */
class JwtAuthenticationFilter(private val jwtTokenProvider: JwtTokenProvider) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val token = resolveToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 토큰으로부터 사용자의 인증 정보(Authentication)를 가져옵니다.
            val authentication = jwtTokenProvider.getAuthentication(token)
            // SecurityContext에 인증 정보를 저장하여, 이후 로직에서 인증된 사용자로 인식하게 합니다.
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        return resolveTokenFromHeader(request) ?: resolveTokenFromCookie(request)
    }

    private fun resolveTokenFromHeader(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION)
        return bearerToken
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)
            ?.takeIf { it.isNotBlank() }
    }

    private fun resolveTokenFromCookie(request: HttpServletRequest): String? {
        return request.cookies?.find { cookie -> cookie.name == ACCESS_TOKEN_COOKIE }?.value
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val ACCESS_TOKEN_COOKIE = "accessToken"
    }
}