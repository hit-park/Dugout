package com.dugout.api.domain.match.dto

import com.dugout.api.domain.match.entity.Match
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MatchResponse(
    val id: Long,
    val teamId: Long,
    val opponentName: String?,
    val opponentTeamId: Long?,
    val groundId: Long?,
    val groundName: String?,
    val matchDate: LocalDate,
    val gatherTime: LocalTime?,
    val matchTime: LocalTime,
    val voteDeadline: LocalDateTime?,
    val status: String,
    val resultHome: Int?,
    val resultAway: Int?,
    val memo: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(match: Match): MatchResponse = MatchResponse(
            id = match.id,
            teamId = match.team.id,
            opponentName = match.opponentName,
            opponentTeamId = match.opponentTeamId,
            groundId = match.groundId,
            groundName = match.groundName,
            matchDate = match.matchDate,
            gatherTime = match.gatherTime,
            matchTime = match.matchTime,
            voteDeadline = match.voteDeadline,
            status = match.status.name,
            resultHome = match.resultHome,
            resultAway = match.resultAway,
            memo = match.memo,
            createdAt = match.createdAt,
        )
    }
}
