package com.dugout.api.global.auth

import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import java.util.Date

/**
 * Apple Sign In 검증 클라이언트.
 * 클라이언트(iOS)가 Apple Identity Token(JWT)을 전달하면,
 * Apple의 JWKS로 서명을 검증하고 클레임에서 사용자 정보를 추출한다.
 */
@Component
class AppleOAuthClient(
    @Value("\${oauth.apple.jwks-url}") private val jwksUrl: String,
    @Value("\${oauth.apple.issuer}") private val issuer: String,
    @Value("\${oauth.apple.client-id}") private val clientId: String,
) : OAuthClient {

    override val provider: AuthProvider = AuthProvider.APPLE

    private val log = LoggerFactory.getLogger(javaClass)

    private val jwtProcessor: DefaultJWTProcessor<SecurityContext> by lazy {
        val jwkSource = RemoteJWKSet<SecurityContext>(URL(jwksUrl))
        val keySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)

        DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = keySelector
            jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
                JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(clientId)
                    .build(),
                setOf("sub", "iss", "aud", "exp", "iat"),
            )
        }
    }

    override fun getUserInfo(token: String): OAuthUserInfo {
        try {
            val parsed = JWTParser.parse(token)
            if (parsed !is SignedJWT) {
                throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
            }

            val claims = jwtProcessor.process(parsed, null)

            if (claims.expirationTime?.before(Date()) == true) {
                throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
            }

            val sub = claims.subject ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
            val email = claims.getStringClaim("email")

            return OAuthUserInfo(
                provider = AuthProvider.APPLE,
                providerId = sub,
                email = email,
                nickname = email?.substringBefore("@") ?: "Apple 사용자",
                profileImageUrl = null,
            )
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("Apple Identity Token 검증 실패", e)
            throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
        }
    }
}
