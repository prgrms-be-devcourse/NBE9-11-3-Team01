package com.team01.backend.domain.user.service

import com.team01.backend.domain.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.Collections

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {
    
    /**
     * 스프링 시큐리티의 입구 서블릿 필터와 연계되어 신원을 조회하는 코어 핵심.
     * 이메일을 기반으로 계정을 파악하고 권한 컬렉션을 안전하게 바인딩하여 반환.
     */
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("이메일을 찾을 수 없습니다 : $email")

        return org.springframework.security.core.userdetails.User(
            user.email,
            user.password,
            Collections.singletonList(SimpleGrantedAuthority(user.role.key))
        )
    }
}