package com.dugout.api.global.auth

import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GoogleOAuthClient(
    @Value("\${oauth.google.user-info-url}") private val userInfoUrl: String,
) : OAuthClient {

    override val provider: AuthProvider = AuthProvider.GOOGLE

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().build()

    override fun getUserInfo(token: String): OAuthUserInfo {
        try {
            val response = webClient.get()
                .uri(userInfoUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .bodyToMono(GoogleApiResponse::class.java)
                .block() ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)

            return OAuthUserInfo(
                provider = AuthProvider.GOOGLE,
                providerId = response.sub,
                email = response.email,
                nickname = response.name ?: response.email?.substringBefore("@") ?: "사용자",
                profileImageUrl = response.picture,
            )
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("구글 사용자 정보 조회 실패", e)
            throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
        }
    }
}

data class GoogleApiResponse(
    val sub: String,
    val email: String?,
    val name: String?,
    val picture: String?,
)
