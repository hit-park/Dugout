package com.dugout.api.global.error

class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
