package com.dugout.api.global.auth

import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class KakaoOAuthClient(
    @Value("\${oauth.kakao.user-info-url}") private val userInfoUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder().build()

    fun getUserInfo(accessToken: String): KakaoUserInfo {
        try {
            val response = webClient.get()
                .uri(userInfoUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .bodyToMono(KakaoApiResponse::class.java)
                .block() ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)

            return KakaoUserInfo(
                id = response.id,
                email = response.kakaoAccount?.email,
                nickname = response.kakaoAccount?.profile?.nickname ?: "사용자",
                profileImageUrl = response.kakaoAccount?.profile?.profileImageUrl,
            )
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("카카오 사용자 정보 조회 실패", e)
            throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
        }
    }
}

data class KakaoUserInfo(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
)

data class KakaoApiResponse(
    val id: Long,
    val kakaoAccount: KakaoAccount?,
) {
    data class KakaoAccount(
        val email: String?,
        val profile: KakaoProfile?,
    )

    data class KakaoProfile(
        val nickname: String?,
        val profileImageUrl: String?,
    )
}
