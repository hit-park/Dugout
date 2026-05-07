package com.dugout.api.domain.lineup.dto

import com.dugout.api.domain.lineup.entity.Lineup
import com.dugout.api.domain.lineup.entity.LineupEntry
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class LineupEntryPayload(
    @field:NotNull(message = "사용자 ID는 필수입니다")
    val userId: Long,

    @field:NotBlank(message = "포지션은 필수입니다")
    val position: String,

    @field:Min(value = 1, message = "타순은 1 이상입니다")
    @field:Max(value = 9, message = "타순은 9 이하입니다")
    val battingOrder: Int? = null,

    val isBench: Boolean = false,
)

data class SaveLineupRequest(
    @field:Valid
    @field:NotNull(message = "라인업 엔트리는 필수입니다")
    val entries: List<LineupEntryPayload>,
)

data class LineupEntryResponse(
    val id: Long,
    val userId: Long,
    val nickname: String,
    val position: String,
    val battingOrder: Int?,
    val isBench: Boolean,
) {
    companion object {
        fun from(entry: LineupEntry): LineupEntryResponse = LineupEntryResponse(
            id = entry.id,
            userId = entry.user.id,
            nickname = entry.user.nickname,
            position = entry.position,
            battingOrder = entry.battingOrder,
            isBench = entry.isBench,
        )
    }
}

data class LineupResponse(
    val id: Long,
    val matchId: Long,
    val teamId: Long,
    val isAiGenerated: Boolean,
    val isConfirmed: Boolean,
    val confirmedAt: LocalDateTime?,
    val entries: List<LineupEntryResponse>,
) {
    companion object {
        fun of(lineup: Lineup, entries: List<LineupEntry>): LineupResponse = LineupResponse(
            id = lineup.id,
            matchId = lineup.match.id,
            teamId = lineup.team.id,
            isAiGenerated = lineup.isAiGenerated,
            isConfirmed = lineup.isConfirmed,
            confirmedAt = lineup.confirmedAt,
            entries = entries.map(LineupEntryResponse::from),
        )
    }
}

data class LineupRecommendationResponse(
    val matchId: Long,
    val isAiGenerated: Boolean,
    val source: String,            // "STUB" | "AI" — Phase 2 전환 시 "AI"로 변경
    val entries: List<LineupEntryPayload>,
)
