package com.team01.backend.global.config

import com.team01.backend.global.security.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.List

/**
 * [과제] Spring Security의 전반적인 보안 정책을 설정하는 클래스입니다.
 * 기존 Java 코드를 기반으로 하이브리드 인증(세션+쿠키) 최적화를 반영했습니다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val accessDeniedHandler: CustomAccessDeniedHandler,
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint
) {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // 1. CSRF 및 CORS 설정
            .csrf { it.disable() } 
            .cors { it.configurationSource(corsConfigurationSource()) }
            
            // 2. [F1 조치] 하이브리드 인증을 위한 세션 정책 수정
            // 기존 STATELESS에서 IF_REQUIRED로 변경하여 필요 시 서버 세션을 활용합니다.
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            
            // 3. 인가(Authorization) 범위 설정
            .authorizeHttpRequests { auth -> auth
                .requestMatchers("/auth/**").permitAll()      // 로그인, 회원가입 등 인증 API
                .requestMatchers("/static/images/**").permitAll() // 프로필 이미지 보급로
                .requestMatchers("/h2-console/**").permitAll()   // DB 콘솔
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // API 문서
                .anyRequest().authenticated()                 // 그 외 모든 요청은 인증 필수
            }
            
            // 4. 보안 헤더 및 필터 배치
            .headers { headers -> 
                headers.frameOptions { it.sameOrigin() } // H2 콘솔 사용을 위한 설정
            }
            // UsernamePasswordAuthenticationFilter 이전에 JWT 인증 필터를 실행
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider), 
                UsernamePasswordAuthenticationFilter::class.java
            )
            
            // 5. 예외 처리(Exception Handling)
            .exceptionHandling { ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)
            }

        return http.build()
    }

    /**
     * CORS 정책 설정: 허용할 도메인과 메서드를 정의합니다.
     */
    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000", "https://cdpn.io")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * 인증 매니저 빈 등록
     */
    @Bean
    @Throws(Exception::class)
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
}