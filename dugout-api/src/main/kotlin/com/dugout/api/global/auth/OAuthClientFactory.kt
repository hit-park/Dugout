package com.dugout.api.global.auth

import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Component

/**
 * AuthProvider에 맞는 OAuthClient 구현체를 선택하는 팩토리.
 * Spring이 OAuthClient를 구현한 모든 Bean을 자동 주입한다.
 */
@Component
class OAuthClientFactory(clients: List<OAuthClient>) {

    private val clientsByProvider: Map<AuthProvider, OAuthClient> =
        clients.associateBy { it.provider }

    fun getClient(provider: AuthProvider): OAuthClient =
        clientsByProvider[provider]
            ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
}
