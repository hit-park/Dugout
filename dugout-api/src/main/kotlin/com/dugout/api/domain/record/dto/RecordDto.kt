package com.dugout.api.domain.record.dto

import com.dugout.api.domain.record.entity.BattingResult
import com.dugout.api.domain.record.entity.PlateAppearance
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreatePlateAppearanceRequest(
    @field:NotNull val matchId: Long,
    @field:NotNull val teamMemberId: Long,
    @field:NotNull val result: BattingResult,
    @field:Min(0) val rbi: Int = 0,
)

data class PlateAppearanceResponse(
    val id: Long,
    val matchId: Long,
    val teamMemberId: Long,
    val result: BattingResult,
    val rbi: Int,
) {
    companion object {
        fun of(pa: PlateAppearance) = PlateAppearanceResponse(
            id = pa.id,
            matchId = pa.match.id,
            teamMemberId = pa.teamMember.id,
            result = pa.result,
            rbi = pa.rbi,
        )
    }
}

data class BattingStatsResponse(
    val teamMemberId: Long,
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val avg: Double,
    val obp: Double,
    val slg: Double,
    val ops: Double,
)
