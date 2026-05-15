package com.team01.backend.global.security

import io.jsonwebtoken.*
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.*

/**
 * JWT(JSON Web Token)의 생성, 추출, 유효성 검증을 담당하는 클래스입니다.
 *
 */
@Component
class JwtTokenProvider(
    // application.properties 또는 yaml에 설정된 비밀키를 가져옵니다.
    @Value("\${jwt.secret}") private var secretKey: String,
    private val userDetailsService: UserDetailsService
) {

    // 액세스 토큰 만료 시간: 15분 (보안 강화를 위해 기존보다 단축됨)
    //
    private val accessTokenValidTime = 15 * 60 * 1000L
    
    // 리프레시 토큰 만료 시간: 7일
    //
    private val refreshTokenValidTime = 7 * 24 * 60 * 60 * 1000L

    /**
     * 객체 생성 후 비밀키를 Base64로 인코딩하여 보안성을 한층 더 높입니다.
     *
     */
    @PostConstruct
    protected fun init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.toByteArray())
    }

    /**
     * 사용자의 이메일과 권한 정보를 담은 액세스 토큰을 생성합니다.
     *
     */
    fun createAccessToken(email: String, role: String): String {
        return createToken(email, role, accessTokenValidTime)
    }

    /**
     * 장기 로그인을 위한 리프레시 토큰을 생성합니다.
     *
     */
    fun createRefreshToken(email: String): String {
        return createToken(email, null, refreshTokenValidTime)
    }

    /**
     * 실제 JWT 토큰을 빌드하는 내부 공통 메서드입니다.
     *
     */
    private fun createToken(email: String, role: String?, validTime: Long): String {
        val claims: Claims = Jwts.claims().setSubject(email)
        if (role != null) {
            claims["role"] = role
        }
        val now = Date()
        return Jwts.builder()
            .setClaims(claims)           // 정보 저장
            .setIssuedAt(now)            // 토큰 발행 시간 정보
            .setExpiration(Date(now.time + validTime)) // 만료 시간 설정
            .signWith(SignatureAlgorithm.HS256, secretKey) // 암호화 알고리즘 설정
            .compact()
    }

    /**
     * 토큰에 담긴 이메일 정보를 바탕으로 DB에서 사용자를 찾아 인증 객체(Authentication)를 반환합니다.
     *
     */
    fun getAuthentication(token: String): Authentication {
        val userDetails = userDetailsService.loadUserByUsername(this.getUserEmail(token))
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    /**
     * 토큰의 'Subject' 영역에서 사용자 이메일을 추출합니다.
     *
     */
    fun getUserEmail(token: String): String {
        // [수정 완료] Deprecated 된 parser() 대신 최신 규격인 parserBuilder() 적용
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body.subject
    }

    /**
     * 토큰의 유효성 및 만료 여부를 확인합니다.
     *
     */
    fun validateToken(jwtToken: String): Boolean {
        return try {
            // [수정 완료] parserBuilder() 적용
            val claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(jwtToken)
            !claims.body.expiration.before(Date())
        } catch (e: Exception) {
            // 토큰이 위조되었거나 만료된 경우 false를 반환합니다.
            false
        }
    }

    /**
     * [추가] 토큰의 남은 만료 시간을 계산합니다. (로그아웃 처리 등에 활용 가능)
     *
     */
    fun getRemainingTime(token: String): Long {
        // [수정 완료] parserBuilder() 적용
        val expiration = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
            .expiration
        return expiration.time - Date().time
    }
}