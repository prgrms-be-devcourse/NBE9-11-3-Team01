package com.team01.backend.global.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

import java.io.IOException;

// 모든 요청에 XssRequestWrapper 적용
@Component
class XssFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        chain.doFilter(XssRequestWrapper(request as HttpServletRequest), response)
    }
}