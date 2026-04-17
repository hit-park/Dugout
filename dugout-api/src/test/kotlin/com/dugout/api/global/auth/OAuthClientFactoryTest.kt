package com.dugout.api.global.auth

import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OAuthClientFactoryTest {

    private class FakeClient(override val provider: AuthProvider) : OAuthClient {
        override fun getUserInfo(token: String): OAuthUserInfo =
            OAuthUserInfo(provider, "id", null, "nick", null)
    }

    @Test
    fun `provider에 맞는 클라이언트 반환`() {
        val kakao = FakeClient(AuthProvider.KAKAO)
        val naver = FakeClient(AuthProvider.NAVER)
        val google = FakeClient(AuthProvider.GOOGLE)
        val apple = FakeClient(AuthProvider.APPLE)

        val factory = OAuthClientFactory(listOf(kakao, naver, google, apple))

        assertSame(kakao, factory.getClient(AuthProvider.KAKAO))
        assertSame(naver, factory.getClient(AuthProvider.NAVER))
        assertSame(google, factory.getClient(AuthProvider.GOOGLE))
        assertSame(apple, factory.getClient(AuthProvider.APPLE))
    }

    @Test
    fun `등록되지 않은 provider 요청 시 OAUTH_PROVIDER_ERROR`() {
        val factory = OAuthClientFactory(listOf(FakeClient(AuthProvider.KAKAO)))

        val exception = assertThrows<BusinessException> {
            factory.getClient(AuthProvider.APPLE)
        }
        assertEquals(ErrorCode.OAUTH_PROVIDER_ERROR, exception.errorCode)
    }
}
