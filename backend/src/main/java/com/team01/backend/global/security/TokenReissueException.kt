package com.team01.backend.global.security

class TokenReissueException(
    val errorCode: String,
    message: String
) : RuntimeException(message)