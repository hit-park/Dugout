package com.dugout.api.domain.user.service

import com.dugout.api.domain.user.dto.AuthResponse
import com.dugout.api.domain.user.dto.OAuthLoginRequest
import com.dugout.api.domain.user.dto.TokenRefreshRequest
import com.dugout.api.domain.user.dto.UserResponse
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.auth.JwtProvider
import com.dugout.api.global.auth.OAuthClientFactory
import com.dugout.api.global.auth.OAuthUserInfo
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val oAuthClientFactory: OAuthClientFactory,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val REFRESH_TOKEN_PREFIX = "refresh:"
        private const val REFRESH_TOKEN_TTL_DAYS = 30L
    }

    @Transactional
    fun oauthLogin(provider: AuthProvider, request: OAuthLoginRequest): AuthResponse {
        val client = oAuthClientFactory.getClient(provider)
        val userInfo = client.getUserInfo(request.accessToken)

        val user = findOrCreateUser(userInfo)
        return issueTokens(user)
    }

    fun refresh(request: TokenRefreshRequest): AuthResponse {
        if (!jwtProvider.validateToken(request.refreshToken)) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        val tokenType = jwtProvider.getTokenType(request.refreshToken)
        if (tokenType != "refresh") {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }

        val userId = jwtProvider.getUserId(request.refreshToken)
        val storedToken = redisTemplate.opsForValue().get("$REFRESH_TOKEN_PREFIX$userId")

        if (storedToken == null || storedToken != request.refreshToken) {
            throw BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        return issueTokens(user)
    }

    fun logout(userId: Long) {
        redisTemplate.delete("$REFRESH_TOKEN_PREFIX$userId")
    }

    private fun findOrCreateUser(userInfo: OAuthUserInfo): User {
        val existing = userRepository.findByProviderAndProviderId(
            userInfo.provider,
            userInfo.providerId,
        )

        if (existing != null) {
            existing.updateProfile(userInfo.nickname, userInfo.profileImageUrl)
            return existing
        }

        return userRepository.save(
            User.create(
                provider = userInfo.provider,
                providerId = userInfo.providerId,
                nickname = userInfo.nickname,
                email = userInfo.email,
                profileImgUrl = userInfo.profileImageUrl,
            ),
        )
    }

    private fun issueTokens(user: User): AuthResponse {
        val accessToken = jwtProvider.createAccessToken(user.id)
        val refreshToken = jwtProvider.createRefreshToken(user.id)

        redisTemplate.opsForValue().set(
            "$REFRESH_TOKEN_PREFIX${user.id}",
            refreshToken,
            REFRESH_TOKEN_TTL_DAYS,
            TimeUnit.DAYS,
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse.from(user),
        )
    }
}
