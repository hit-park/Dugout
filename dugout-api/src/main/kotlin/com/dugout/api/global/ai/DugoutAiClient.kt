package com.dugout.api.global.ai

import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException

/**
 * dugout-ai (FastAPI) HTTP 어댑터.
 *
 * - 모든 메서드는 동기 호출 (Spring MVC 환경 가정)
 * - 연결 실패: AI_SERVICE_UNAVAILABLE (503)
 * - 응답 4xx/5xx: AI_REQUEST_FAILED (502)
 *
 * 운영 시 Resilience4j retry/circuit breaker 추가 검토 (Phase 2.5).
 */
@Component
class DugoutAiClient(
    private val dugoutAiRestClient: RestClient,
) {
    private val log = LoggerFactory.getLogger(DugoutAiClient::class.java)

    fun predictAttendance(request: AiAttendanceContext): AiAttendancePrediction =
        post("/api/attendance/predict", request, AiAttendancePrediction::class.java)

    fun recommendLineup(request: AiLineupRecommendRequest): AiLineupRecommendResponse =
        post("/api/lineups/recommend", request, AiLineupRecommendResponse::class.java)

    fun computeMatchingScore(request: AiMatchingScoreRequest): AiMatchingScoreResponse =
        post("/api/matching/score", request, AiMatchingScoreResponse::class.java)

    fun recommendMercenary(request: AiMercenaryRecommendRequest): AiMercenaryRecommendResponse =
        post("/api/mercenary/recommend", request, AiMercenaryRecommendResponse::class.java)

    private fun <T> post(path: String, body: Any, responseType: Class<T>): T = try {
        dugoutAiRestClient.post()
            .uri(path)
            .body(body)
            .retrieve()
            .body(responseType)
            ?: throw BusinessException(ErrorCode.AI_REQUEST_FAILED)
    } catch (e: ResourceAccessException) {
        log.warn("dugout-ai 연결 실패: {}", e.message)
        throw BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE)
    } catch (e: RestClientResponseException) {
        log.warn("dugout-ai 비정상 응답: status={} body={}", e.statusCode, e.responseBodyAsString.take(200))
        throw BusinessException(ErrorCode.AI_REQUEST_FAILED)
    }
}
