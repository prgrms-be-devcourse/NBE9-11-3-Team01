package com.team01.backend.global.error

import com.team01.backend.global.response.ApiResponse
import com.team01.backend.global.security.TokenReissueException
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 기본 예외 처리. 응답 본문은 항상 [ApiResponse] 형태로 맞춘다. */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Void>> {
        val detail = ex.bindingResult.fieldErrors
            .joinToString(" / ") { err ->
                "${err.field}: ${err.defaultMessage ?: "(사유 없음)"}"
            }
        val message = if (detail.isEmpty()) {
            "입력값이 올바르지 않습니다."
        } else {
            "입력값이 올바르지 않습니다. ($detail)"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.ofFailure("INVALID_INPUT", message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Void>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponse.ofFailure(
                    "INVALID_INPUT",
                    ex.message ?: "요청을 처리할 수 없습니다.",
                ),
            )
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<ApiResponse<Void>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.ofFailure("NOT_FOUND", "요청하신 데이터를 찾을 수 없습니다."))
    }

    @ExceptionHandler(TokenReissueException::class)
    fun handleTokenReissue(ex: TokenReissueException): ResponseEntity<ApiResponse<Void>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                ApiResponse.ofFailure(
                    ex.errorCode,
                    ex.message ?: "인증 정보가 유효하지 않습니다.",
                ),
            )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ResponseEntity<ApiResponse<Void>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                ApiResponse.ofFailure(
                    "UNAUTHORIZED",
                    ex.message ?: "인증 정보가 유효하지 않습니다.",
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Void>> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiResponse.ofFailure(
                    "INTERNAL_ERROR",
                    "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
                ),
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Void>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.ofFailure("INVALID_JSON", "잘못된 형식의 JSON 데이터입니다."))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Void>> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.ofFailure("FORBIDDEN", ex.message))
    }
}
