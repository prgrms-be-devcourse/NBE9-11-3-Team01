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
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("이메일을 찾을 수 없습니다 : $email") }

        return org.springframework.security.core.userdetails.User(
            user.email,
            user.password,
            Collections.singletonList(SimpleGrantedAuthority(user.role.key))
        )
    }
}
