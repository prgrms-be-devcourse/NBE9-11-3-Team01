package com.team01.backend.global.filter;

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.StringReader

// 요청 파라미터의 XSS 방지 처리를 위한 래퍼 클래스
class XssRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    override fun getParameter(name: String?): String? =
        sanitize(super.getParameter(name))

    override fun getParameterValues(name: String?): Array<String?>? =
        super.getParameterValues(name)?.map { sanitize(it) }?.toTypedArray()

    override fun getReader(): BufferedReader {
        val sanitized = sanitize(request.inputStream.bufferedReader().readText())
        return BufferedReader(StringReader(sanitized ?: ""))
    }

    override fun getInputStream(): ServletInputStream {
        val sanitized = sanitize(request.inputStream.bufferedReader().readText()) ?: ""
        val bytes = sanitized.toByteArray()
        return object : ServletInputStream() {
            private val stream = ByteArrayInputStream(bytes)
            override fun read() = stream.read()
            override fun isFinished() = stream.available() == 0
            override fun isReady() = true
            override fun setReadListener(listener: ReadListener) {}
        }
    }

    private fun sanitize(value: String?): String? =
        value?.replace("&", "&amp;")
            ?.replace("<", "&lt;")
            ?.replace(">", "&gt;")
}
