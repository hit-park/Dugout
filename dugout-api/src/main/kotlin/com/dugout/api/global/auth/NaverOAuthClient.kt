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
class NaverOAuthClient(
    @Value("\${oauth.naver.user-info-url}") private val userInfoUrl: String,
) : OAuthClient {

    override val provider: AuthProvider = AuthProvider.NAVER

    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().build()

    override fun getUserInfo(token: String): OAuthUserInfo {
        try {
            val response = webClient.get()
                .uri(userInfoUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .bodyToMono(NaverApiResponse::class.java)
                .block() ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)

            if (response.resultcode != "00" || response.response == null) {
                log.warn("네이버 사용자 정보 조회 실패: code={}, message={}", response.resultcode, response.message)
                throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
            }

            val profile = response.response
            return OAuthUserInfo(
                provider = AuthProvider.NAVER,
                providerId = profile.id,
                email = profile.email,
                nickname = profile.nickname ?: profile.name ?: "사용자",
                profileImageUrl = profile.profileImage,
            )
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("네이버 사용자 정보 조회 실패", e)
            throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
        }
    }
}

data class NaverApiResponse(
    val resultcode: String,
    val message: String?,
    val response: NaverProfile?,
) {
    data class NaverProfile(
        val id: String,
        val email: String?,
        val nickname: String?,
        val name: String?,
        val profileImage: String?,
    )
}
