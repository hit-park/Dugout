package com.dugout.api.global.auth

import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiry}") private val accessTokenExpiry: Long,
    @Value("\${jwt.refresh-token-expiry}") private val refreshTokenExpiry: Long,
) {
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun createAccessToken(userId: Long): String = createToken(userId, "access", accessTokenExpiry)

    fun createRefreshToken(userId: Long): String = createToken(userId, "refresh", refreshTokenExpiry)

    fun validateToken(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: Exception) {
            false
        }

    fun getUserId(token: String): Long =
        try {
            parseClaims(token).subject.toLong()
        } catch (e: ExpiredJwtException) {
            throw BusinessException(ErrorCode.TOKEN_EXPIRED)
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

    fun getTokenType(token: String): String =
        try {
            parseClaims(token)["type"] as String
        } catch (e: ExpiredJwtException) {
            throw BusinessException(ErrorCode.TOKEN_EXPIRED)
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

    private fun createToken(userId: Long, type: String, expiry: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(now)
            .expiration(Date(now.time + expiry))
            .signWith(key)
            .compact()
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
