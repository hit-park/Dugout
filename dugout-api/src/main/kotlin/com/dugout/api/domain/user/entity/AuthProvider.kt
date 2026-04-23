package com.dugout.api.domain.user.entity

enum class AuthProvider {
    KAKAO,
    NAVER,
    APPLE,
    GOOGLE,

    /**
     * 로컬/개발 환경에서만 사용하는 가짜 provider.
     * 운영 환경에는 절대 노출되지 않아야 한다 (DevAuthController가 @Profile("local")로 제한됨).
     */
    DEV,
}
