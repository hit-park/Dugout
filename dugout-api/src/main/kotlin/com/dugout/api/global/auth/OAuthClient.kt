package com.dugout.api.global.auth

import com.dugout.api.domain.user.entity.AuthProvider

/**
 * OAuth 제공자 공통 인터페이스.
 * Kakao, Naver, Google은 access token을 받아 제공자 API를 호출하고,
 * Apple은 identity token(JWT)을 직접 검증한다.
 */
interface OAuthClient {
    val provider: AuthProvider
    fun getUserInfo(token: String): OAuthUserInfo
}
