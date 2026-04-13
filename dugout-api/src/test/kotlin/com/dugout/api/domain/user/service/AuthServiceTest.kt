package com.dugout.api.domain.user.service

import com.dugout.api.domain.user.dto.OAuthLoginRequest
import com.dugout.api.domain.user.dto.TokenRefreshRequest
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.auth.JwtProvider
import com.dugout.api.global.auth.KakaoOAuthClient
import com.dugout.api.global.auth.KakaoUserInfo
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.Optional
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var jwtProvider: JwtProvider
    @Mock lateinit var kakaoOAuthClient: KakaoOAuthClient
    @Mock lateinit var redisTemplate: StringRedisTemplate
    @Mock lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        org.mockito.Mockito.lenient().`when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        authService = AuthService(userRepository, jwtProvider, kakaoOAuthClient, redisTemplate)
    }

    @Test
    fun `카카오 로그인 - 신규 사용자 생성`() {
        val kakaoUserInfo = KakaoUserInfo(
            id = 12345L,
            email = "test@kakao.com",
            nickname = "김주장",
            profileImageUrl = "https://example.com/photo.jpg",
        )
        whenever(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUserInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { invocation ->
            val user = invocation.getArgument<User>(0)
            user
        }
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.kakaoLogin(OAuthLoginRequest("kakao-token"))

        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals("김주장", response.user.nickname)
        assertEquals("KAKAO", response.user.provider)
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `카카오 로그인 - 기존 사용자 프로필 업데이트`() {
        val existingUser = User.create(
            provider = AuthProvider.KAKAO,
            providerId = "12345",
            nickname = "이전닉네임",
            email = "test@kakao.com",
        )
        val kakaoUserInfo = KakaoUserInfo(
            id = 12345L,
            email = "test@kakao.com",
            nickname = "새닉네임",
            profileImageUrl = "https://example.com/new-photo.jpg",
        )
        whenever(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUserInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345")).thenReturn(existingUser)
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.kakaoLogin(OAuthLoginRequest("kakao-token"))

        assertEquals("새닉네임", response.user.nickname)
    }

    @Test
    fun `토큰 갱신 성공`() {
        whenever(jwtProvider.validateToken("valid-refresh")).thenReturn(true)
        whenever(jwtProvider.getTokenType("valid-refresh")).thenReturn("refresh")
        whenever(jwtProvider.getUserId("valid-refresh")).thenReturn(1L)
        whenever(valueOperations.get("refresh:1")).thenReturn("valid-refresh")

        val user = User.create(AuthProvider.KAKAO, "12345", "김주장")
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(jwtProvider.createAccessToken(any())).thenReturn("new-access")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("new-refresh")

        val response = authService.refresh(TokenRefreshRequest("valid-refresh"))

        assertEquals("new-access", response.accessToken)
        assertEquals("new-refresh", response.refreshToken)
    }

    @Test
    fun `토큰 갱신 - 유효하지 않은 토큰이면 INVALID_TOKEN 에러`() {
        whenever(jwtProvider.validateToken("bad-token")).thenReturn(false)

        val exception = assertThrows<BusinessException> {
            authService.refresh(TokenRefreshRequest("bad-token"))
        }
        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `토큰 갱신 - Redis에 없는 토큰이면 REFRESH_TOKEN_NOT_FOUND 에러`() {
        whenever(jwtProvider.validateToken("old-refresh")).thenReturn(true)
        whenever(jwtProvider.getTokenType("old-refresh")).thenReturn("refresh")
        whenever(jwtProvider.getUserId("old-refresh")).thenReturn(1L)
        whenever(valueOperations.get("refresh:1")).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            authService.refresh(TokenRefreshRequest("old-refresh"))
        }
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `토큰 갱신 - access 타입 토큰으로 갱신 시도하면 INVALID_TOKEN 에러`() {
        whenever(jwtProvider.validateToken("access-token")).thenReturn(true)
        whenever(jwtProvider.getTokenType("access-token")).thenReturn("access")

        val exception = assertThrows<BusinessException> {
            authService.refresh(TokenRefreshRequest("access-token"))
        }
        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `로그아웃 - Redis에서 리프레시 토큰 삭제`() {
        authService.logout(1L)
        verify(redisTemplate).delete("refresh:1")
    }
}
