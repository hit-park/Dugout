package com.dugout.api.global.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

@Component
class JwtFilter(
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    companion object {
        private val PERMIT_PATHS = listOf(
            "/api/v1/auth/kakao",
            "/api/v1/auth/naver",
            "/api/v1/auth/google",
            "/api/v1/auth/apple",
            "/api/v1/auth/refresh",
            "/api/v1/auth/dev-login",
            "/api/v1/health",
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return PERMIT_PATHS.any { request.requestURI == it }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null) {
            if (jwtProvider.validateToken(token)) {
                val userId = jwtProvider.getUserId(token)
                val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            } else {
                writeErrorResponse(response)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return if (bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }

    private fun writeErrorResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = mapOf(
            "code" to "INVALID_TOKEN",
            "message" to "유효하지 않은 토큰입니다",
            "timestamp" to Instant.now().toString(),
        )
        objectMapper.writeValue(response.writer, body)
    }
}
