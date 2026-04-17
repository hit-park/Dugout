package com.dugout.api.domain.user.controller

import com.dugout.api.domain.user.dto.AuthResponse
import com.dugout.api.domain.user.dto.UserResponse
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.service.AuthService
import com.dugout.api.global.auth.JwtFilter
import com.dugout.api.global.auth.JwtProvider
import com.dugout.api.global.config.SecurityConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, JwtFilter::class)
class AuthControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @MockitoBean lateinit var authService: AuthService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val sampleAuthResponse = AuthResponse(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        user = UserResponse(
            id = 1L,
            email = "test@kakao.com",
            nickname = "김주장",
            profileImgUrl = null,
            provider = "KAKAO",
        ),
    )

    @Test
    fun `POST auth kakao - 카카오 로그인 성공`() {
        whenever(authService.oauthLogin(eq(AuthProvider.KAKAO), any())).thenReturn(sampleAuthResponse)

        mockMvc.perform(
            post("/api/v1/auth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"access_token": "kakao-access-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access-token"))
            .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
            .andExpect(jsonPath("$.user.nickname").value("김주장"))
    }

    @Test
    fun `POST auth naver - 네이버 로그인 성공`() {
        whenever(authService.oauthLogin(eq(AuthProvider.NAVER), any())).thenReturn(sampleAuthResponse)

        mockMvc.perform(
            post("/api/v1/auth/naver")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"access_token": "naver-access-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access-token"))
    }

    @Test
    fun `POST auth google - 구글 로그인 성공`() {
        whenever(authService.oauthLogin(eq(AuthProvider.GOOGLE), any())).thenReturn(sampleAuthResponse)

        mockMvc.perform(
            post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"access_token": "google-access-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access-token"))
    }

    @Test
    fun `POST auth apple - 애플 로그인 성공`() {
        whenever(authService.oauthLogin(eq(AuthProvider.APPLE), any())).thenReturn(sampleAuthResponse)

        mockMvc.perform(
            post("/api/v1/auth/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"access_token": "apple-identity-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access-token"))
    }

    @Test
    fun `POST auth kakao - accessToken 누락 시 400`() {
        mockMvc.perform(
            post("/api/v1/auth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"access_token": ""}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST auth refresh - 토큰 갱신 성공`() {
        whenever(authService.refresh(any())).thenReturn(sampleAuthResponse)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token": "valid-refresh-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access-token"))
    }

    @Test
    fun `DELETE auth logout - 인증 없이 접근 시 401 또는 403`() {
        mockMvc.perform(delete("/api/v1/auth/logout"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `DELETE auth logout - 인증 있으면 성공`() {
        whenever(jwtProvider.validateToken("valid-token")).thenReturn(true)
        whenever(jwtProvider.getUserId("valid-token")).thenReturn(1L)

        mockMvc.perform(
            delete("/api/v1/auth/logout")
                .header("Authorization", "Bearer valid-token"),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `인증 필요한 엔드포인트 - 토큰 없으면 401 또는 403`() {
        mockMvc.perform(post("/api/v1/teams"))
            .andExpect(status().is4xxClientError)
    }
}
