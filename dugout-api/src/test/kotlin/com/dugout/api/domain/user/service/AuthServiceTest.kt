package com.dugout.api.domain.user.service

import com.dugout.api.domain.user.dto.OAuthLoginRequest
import com.dugout.api.domain.user.dto.TokenRefreshRequest
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.auth.JwtProvider
import com.dugout.api.global.auth.OAuthClient
import com.dugout.api.global.auth.OAuthClientFactory
import com.dugout.api.global.auth.OAuthUserInfo
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
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

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var jwtProvider: JwtProvider
    @Mock lateinit var oAuthClientFactory: OAuthClientFactory
    @Mock lateinit var oAuthClient: OAuthClient
    @Mock lateinit var redisTemplate: StringRedisTemplate
    @Mock lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        org.mockito.Mockito.lenient().`when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        authService = AuthService(userRepository, jwtProvider, oAuthClientFactory, redisTemplate)
    }

    @Test
    fun `카카오 로그인 - 신규 사용자 생성`() {
        val userInfo = OAuthUserInfo(
            provider = AuthProvider.KAKAO,
            providerId = "12345",
            email = "test@kakao.com",
            nickname = "김주장",
            profileImageUrl = "https://example.com/photo.jpg",
        )
        whenever(oAuthClientFactory.getClient(AuthProvider.KAKAO)).thenReturn(oAuthClient)
        whenever(oAuthClient.getUserInfo("kakao-token")).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.oauthLogin(AuthProvider.KAKAO, OAuthLoginRequest("kakao-token"))

        assertEquals("access-token", response.accessToken)
        assertEquals("김주장", response.user.nickname)
        assertEquals("KAKAO", response.user.provider)
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `네이버 로그인 - 신규 사용자 생성`() {
        val userInfo = OAuthUserInfo(
            provider = AuthProvider.NAVER,
            providerId = "naver-123",
            email = "test@naver.com",
            nickname = "박선수",
            profileImageUrl = null,
        )
        whenever(oAuthClientFactory.getClient(AuthProvider.NAVER)).thenReturn(oAuthClient)
        whenever(oAuthClient.getUserInfo("naver-token")).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.NAVER, "naver-123")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.oauthLogin(AuthProvider.NAVER, OAuthLoginRequest("naver-token"))

        assertEquals("박선수", response.user.nickname)
        assertEquals("NAVER", response.user.provider)
    }

    @Test
    fun `구글 로그인 - 신규 사용자 생성`() {
        val userInfo = OAuthUserInfo(
            provider = AuthProvider.GOOGLE,
            providerId = "google-abc",
            email = "test@gmail.com",
            nickname = "이용병",
            profileImageUrl = "https://lh3.googleusercontent.com/photo",
        )
        whenever(oAuthClientFactory.getClient(AuthProvider.GOOGLE)).thenReturn(oAuthClient)
        whenever(oAuthClient.getUserInfo("google-token")).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-abc")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.oauthLogin(AuthProvider.GOOGLE, OAuthLoginRequest("google-token"))

        assertEquals("이용병", response.user.nickname)
        assertEquals("GOOGLE", response.user.provider)
    }

    @Test
    fun `애플 로그인 - 신규 사용자 생성`() {
        val userInfo = OAuthUserInfo(
            provider = AuthProvider.APPLE,
            providerId = "apple-xyz",
            email = "test@privaterelay.appleid.com",
            nickname = "test",
            profileImageUrl = null,
        )
        whenever(oAuthClientFactory.getClient(AuthProvider.APPLE)).thenReturn(oAuthClient)
        whenever(oAuthClient.getUserInfo("apple-identity-token")).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.APPLE, "apple-xyz")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.oauthLogin(AuthProvider.APPLE, OAuthLoginRequest("apple-identity-token"))

        assertEquals("APPLE", response.user.provider)
    }

    @Test
    fun `OAuth 로그인 - 기존 사용자 프로필 업데이트`() {
        val existingUser = User.create(
            provider = AuthProvider.KAKAO,
            providerId = "12345",
            nickname = "이전닉네임",
            email = "test@kakao.com",
        )
        val userInfo = OAuthUserInfo(
            provider = AuthProvider.KAKAO,
            providerId = "12345",
            email = "test@kakao.com",
            nickname = "새닉네임",
            profileImageUrl = "https://example.com/new-photo.jpg",
        )
        whenever(oAuthClientFactory.getClient(AuthProvider.KAKAO)).thenReturn(oAuthClient)
        whenever(oAuthClient.getUserInfo("kakao-token")).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345")).thenReturn(existingUser)
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.oauthLogin(AuthProvider.KAKAO, OAuthLoginRequest("kakao-token"))

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

    @Test
    fun `개발 로그인 - 신규 사용자 생성 (DEV provider)`() {
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.DEV, "dev-김주장"))
            .thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.devLogin("김주장")

        assertEquals("김주장", response.user.nickname)
        assertEquals("DEV", response.user.provider)
        assertEquals("access-token", response.accessToken)
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `개발 로그인 - 같은 닉네임으로 재로그인 시 기존 사용자 사용`() {
        val existing = User.create(
            provider = AuthProvider.DEV,
            providerId = "dev-김주장",
            nickname = "김주장",
        )
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.DEV, "dev-김주장"))
            .thenReturn(existing)
        whenever(jwtProvider.createAccessToken(any())).thenReturn("access-token")
        whenever(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token")

        val response = authService.devLogin("김주장")

        assertEquals("김주장", response.user.nickname)
        // 기존 사용자라 save 호출되지 않음
        verify(userRepository, org.mockito.Mockito.never()).save(any<User>())
    }
}
