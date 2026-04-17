package com.dugout.api.domain.user.controller

import com.dugout.api.domain.user.dto.AuthResponse
import com.dugout.api.domain.user.dto.OAuthLoginRequest
import com.dugout.api.domain.user.dto.TokenRefreshRequest
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/kakao")
    fun kakaoLogin(@Valid @RequestBody request: OAuthLoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.oauthLogin(AuthProvider.KAKAO, request))

    @PostMapping("/naver")
    fun naverLogin(@Valid @RequestBody request: OAuthLoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.oauthLogin(AuthProvider.NAVER, request))

    @PostMapping("/google")
    fun googleLogin(@Valid @RequestBody request: OAuthLoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.oauthLogin(AuthProvider.GOOGLE, request))

    @PostMapping("/apple")
    fun appleLogin(@Valid @RequestBody request: OAuthLoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.oauthLogin(AuthProvider.APPLE, request))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: TokenRefreshRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refresh(request))

    @DeleteMapping("/logout")
    fun logout(@AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        authService.logout(userId)
        return ResponseEntity.noContent().build()
    }
}
