package com.dugout.api.global.auth

import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JwtProviderTest {

    private lateinit var jwtProvider: JwtProvider

    @BeforeEach
    fun setUp() {
        jwtProvider = JwtProvider(
            secret = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            accessTokenExpiry = 900_000L,
            refreshTokenExpiry = 2_592_000_000L,
        )
        jwtProvider.init()
    }

    @Test
    fun `Access Token 생성 및 검증 성공`() {
        val userId = 1L
        val token = jwtProvider.createAccessToken(userId)

        assertTrue(jwtProvider.validateToken(token))
        assertEquals(userId, jwtProvider.getUserId(token))
        assertEquals("access", jwtProvider.getTokenType(token))
    }

    @Test
    fun `Refresh Token 생성 및 검증 성공`() {
        val userId = 42L
        val token = jwtProvider.createRefreshToken(userId)

        assertTrue(jwtProvider.validateToken(token))
        assertEquals(userId, jwtProvider.getUserId(token))
        assertEquals("refresh", jwtProvider.getTokenType(token))
    }

    @Test
    fun `만료된 토큰은 validateToken이 false 반환`() {
        val expiredProvider = JwtProvider(
            secret = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            accessTokenExpiry = -1000L,
            refreshTokenExpiry = -1000L,
        )
        expiredProvider.init()

        val token = expiredProvider.createAccessToken(1L)
        assertFalse(jwtProvider.validateToken(token))
    }

    @Test
    fun `만료된 토큰으로 getUserId 호출 시 TOKEN_EXPIRED 에러`() {
        val expiredProvider = JwtProvider(
            secret = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            accessTokenExpiry = -1000L,
            refreshTokenExpiry = -1000L,
        )
        expiredProvider.init()

        val token = expiredProvider.createAccessToken(1L)
        val exception = assertThrows<BusinessException> { jwtProvider.getUserId(token) }
        assertEquals(ErrorCode.TOKEN_EXPIRED, exception.errorCode)
    }

    @Test
    fun `잘못된 토큰은 validateToken이 false 반환`() {
        assertFalse(jwtProvider.validateToken("invalid.token.value"))
        assertFalse(jwtProvider.validateToken(""))
    }

    @Test
    fun `잘못된 토큰으로 getUserId 호출 시 INVALID_TOKEN 에러`() {
        val exception = assertThrows<BusinessException> { jwtProvider.getUserId("invalid.token") }
        assertEquals(ErrorCode.INVALID_TOKEN, exception.errorCode)
    }

    @Test
    fun `다른 secret으로 서명된 토큰은 검증 실패`() {
        val otherProvider = JwtProvider(
            secret = "another-secret-key-that-is-also-at-least-256-bits-long-for-testing",
            accessTokenExpiry = 900_000L,
            refreshTokenExpiry = 2_592_000_000L,
        )
        otherProvider.init()

        val token = otherProvider.createAccessToken(1L)
        assertFalse(jwtProvider.validateToken(token))
    }
}
