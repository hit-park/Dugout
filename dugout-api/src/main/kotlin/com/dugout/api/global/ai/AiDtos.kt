package com.dugout.api.global.ai

/**
 * dugout-ai 서비스 호환 DTO. Pydantic v2 응답 스키마와 1:1 매칭.
 * snake_case 필드는 application.yml의 jackson naming strategy로 자동 매핑된다.
 */

// ─── Attendance ──────────────────────────────────────────────────────────────

data class AiAttendanceContext(
    val userId: Long,
    val matchId: Long,
    val dayOfWeekRate: Double,
    val recent5AttendanceRate: Double,
    val consecutiveAbsentCount: Int,
    val distanceKm: Double,
    val hasBadWeather: Boolean = false,
    val avgResponseLagHours: Double = 0.0,
)

data class AiAttendancePrediction(
    val userId: Long,
    val matchId: Long,
    val prediction: String,
    val probability: Double,
    val confidence: Double,
    val reasons: List<String> = emptyList(),
)

// ─── Lineup ──────────────────────────────────────────────────────────────────

data class AiAttendeeProfile(
    val userId: Long,
    val primaryPosition: String,
    val subPositions: List<String> = emptyList(),
    val benchRatioRecent: Double = 0.0,
    val batsLeft: Boolean = false,
    val singles: Int = 0,
    val doubles: Int = 0,
    val triples: Int = 0,
    val homeRuns: Int = 0,
    val walks: Int = 0,
    val hitByPitch: Int = 0,
    val sacrificeFlies: Int = 0,
    val strikeouts: Int = 0,
    val inPlayOuts: Int = 0,
    val reachedOnErrors: Int = 0,
)

data class AiLineupRecommendRequest(
    val matchId: Long,
    val attendees: List<AiAttendeeProfile>,
    val lineupMode: String = "BALANCED",
)

data class AiLineupAssignment(
    val userId: Long,
    val position: String,
    val battingOrder: Int? = null,
    val isBench: Boolean = false,
)

data class AiLineupRecommendResponse(
    val matchId: Long,
    val isAiGenerated: Boolean = true,
    val source: String = "AI",
    val fairnessNote: String? = null,
    val entries: List<AiLineupAssignment>,
)

// ─── Matching ────────────────────────────────────────────────────────────────

data class AiMatchingScoreRequest(
    val homeElo: Int,
    val awayElo: Int,
    val distanceKm: Double,
    val timeOverlapRatio: Double,
    val awayMannerScore: Double,
)

data class AiMatchingScoreBreakdown(
    val skill: Double,
    val distance: Double,
    val time: Double,
    val manner: Double,
)

data class AiMatchingScoreResponse(
    val totalScore: Double,
    val breakdown: AiMatchingScoreBreakdown,
)

// ─── Mercenary ───────────────────────────────────────────────────────────────

/**
 * dugout-ai로 보내는 후보. PII(닉네임 등)는 절대 미포함 — user_id 만 식별자로 사용.
 * 응답을 받은 뒤 dugout-api 쪽에서 user_id → 닉네임 매핑.
 */
data class AiMercenaryCandidate(
    val userId: Long,
    val regions: List<String> = emptyList(),
    val positions: List<String> = emptyList(),
    val availableDays: List<String> = emptyList(),
    val rating: Double = 0.0,
    val totalGames: Int = 0,
)

data class AiMercenaryRecommendRequest(
    val requestId: Long,
    val neededPositions: List<String>,
    val neededRegions: List<String> = emptyList(),
    val availableDays: List<String> = emptyList(),
    val candidates: List<AiMercenaryCandidate> = emptyList(),
)

data class AiMercenaryMatch(
    val userId: Long,
    val score: Double,
    val matchedPositions: List<String>,
    val matchedRegions: List<String>,
)

data class AiMercenaryRecommendResponse(
    val requestId: Long,
    val matches: List<AiMercenaryMatch>,
)
