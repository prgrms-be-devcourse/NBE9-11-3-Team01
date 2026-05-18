package com.team01.backend.global.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * API 공통 응답 형태. 성공·실패 모두 동일한 JSON 필드로 맞춘다.
 *
 * - 성공: [ofSuccess], [ofSuccessWithoutBody]
 * - 실패: [com.team01.backend.global.error.GlobalExceptionHandler]가 [ofFailure]로 채움 (컨트롤러에서 직접 써도 됨)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
        val success: Boolean,
        val code: String?,
        val message: String?,
        val data: T?,
        ) {
    companion object {
        fun <T> ofSuccess(data: T): ApiResponse<T> =
        ApiResponse(success = true, code = null, message = null, data = data)

        fun ofSuccessWithoutBody(): ApiResponse<Void> =
        ApiResponse(success = true, code = null, message = null, data = null)

        fun <T> ofFailure(code: String, message: String): ApiResponse<T> =
        ApiResponse(success = false, code = code, message = message, data = null)

        fun <T> ofFailure(code: String, message: String, data: T): ApiResponse<T> =
        ApiResponse(success = false, code = code, message = message, data = data)
    }
}