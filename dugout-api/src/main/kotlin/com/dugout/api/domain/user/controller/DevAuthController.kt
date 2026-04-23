package com.dugout.api.domain.user.controller

import com.dugout.api.domain.user.dto.AuthResponse
import com.dugout.api.domain.user.dto.DevLoginRequest
import com.dugout.api.domain.user.service.AuthService
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발 전용 인증 엔드포인트.
 * @Profile("local")로 로컬 프로필에서만 빈 등록됨.
 * 운영/스테이징 환경에서는 절대 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("local")
class DevAuthController(
    private val authService: AuthService,
) {
    /**
     * 닉네임만 받아 즉시 JWT를 발급.
     * OAuth 제공자 검증을 건너뛰므로 카카오 SDK 없이 iOS 로그인 플로우를 검증할 수 있다.
     */
    @PostMapping("/dev-login")
    fun devLogin(@Valid @RequestBody request: DevLoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.devLogin(request.nickname))
}
