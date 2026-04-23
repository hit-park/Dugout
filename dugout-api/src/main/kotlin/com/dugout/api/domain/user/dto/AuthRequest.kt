package com.dugout.api.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OAuthLoginRequest(
    @field:NotBlank(message = "액세스 토큰은 필수입니다")
    val accessToken: String,
)

data class TokenRefreshRequest(
    @field:NotBlank(message = "리프레시 토큰은 필수입니다")
    val refreshToken: String,
)

/**
 * 개발 전용 로그인 요청. 로컬 프로필에서만 허용.
 */
data class DevLoginRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(max = 50, message = "닉네임은 50자 이내로 입력해주세요")
    val nickname: String,
)
