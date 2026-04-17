package com.dugout.api.global.auth

import com.dugout.api.domain.user.entity.AuthProvider

/**
 * OAuth 제공자별 사용자 정보를 통일된 형태로 표현하는 DTO.
 * 각 OAuthClient 구현체는 제공자의 응답을 이 객체로 변환해야 한다.
 */
data class OAuthUserInfo(
    val provider: AuthProvider,
    val providerId: String,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
)
