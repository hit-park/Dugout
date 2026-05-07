package com.dugout.api.domain.matching.dto

import com.dugout.api.domain.matching.entity.HomeAway
import com.dugout.api.domain.matching.entity.MatchingProposal
import com.dugout.api.domain.matching.entity.MatchingRequest
import com.dugout.api.domain.matching.entity.TeamRating
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class CreateMatchingRequestPayload(
    @field:NotEmpty(message = "선호 날짜는 1개 이상이어야 합니다")
    val preferredDates: List<String>,

    val preferredTime: LocalTime? = null,
    val preferredGroundId: Long? = null,
    val preferredRegion: String? = null,
    val ratingMin: Int? = null,
    val ratingMax: Int? = null,
    val homeAway: HomeAway = HomeAway.ANY,
    val expiresAt: LocalDateTime? = null,
)

data class ProposeMatchPayload(
    val proposedDate: LocalDate? = null,
    val proposedTime: LocalTime? = null,
    val proposedGroundId: Long? = null,
    val memo: String? = null,
)

data class MatchResultPayload(
    @field:NotNull(message = "홈 점수는 필수입니다")
    val homeScore: Int,

    @field:NotNull(message = "원정 점수는 필수입니다")
    val awayScore: Int,
)

data class TeamRatingResponse(
    val teamId: Long,
    val teamName: String,
    val eloRating: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val mannerScore: Double,
    val mannerCount: Int,
) {
    companion object {
        fun from(rating: TeamRating): TeamRatingResponse = TeamRatingResponse(
            teamId = rating.team.id,
            teamName = rating.team.name,
            eloRating = rating.eloRating,
            wins = rating.wins,
            losses = rating.losses,
            draws = rating.draws,
            mannerScore = rating.mannerScore,
            mannerCount = rating.mannerCount,
        )
    }
}

data class MatchingRequestResponse(
    val id: Long,
    val teamId: Long,
    val teamName: String,
    val preferredDates: List<String>,
    val preferredTime: LocalTime?,
    val preferredGroundId: Long?,
    val preferredRegion: String?,
    val ratingMin: Int?,
    val ratingMax: Int?,
    val homeAway: String,
    val matchedTeamId: Long?,
    val matchDate: LocalDate?,
    val matchTime: LocalTime?,
    val groundId: Long?,
    val resultHomeScore: Int?,
    val resultAwayScore: Int?,
    val status: String,
    val expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(request: MatchingRequest): MatchingRequestResponse = MatchingRequestResponse(
            id = request.id,
            teamId = request.team.id,
            teamName = request.team.name,
            preferredDates = request.preferredDates,
            preferredTime = request.preferredTime,
            preferredGroundId = request.preferredGroundId,
            preferredRegion = request.preferredRegion,
            ratingMin = request.ratingMin,
            ratingMax = request.ratingMax,
            homeAway = request.homeAway.name,
            matchedTeamId = request.matchedTeamId,
            matchDate = request.matchDate,
            matchTime = request.matchTime,
            groundId = request.groundId,
            resultHomeScore = request.resultHomeScore,
            resultAwayScore = request.resultAwayScore,
            status = request.status.name,
            expiresAt = request.expiresAt,
            createdAt = request.createdAt,
        )
    }
}

data class MatchingProposalResponse(
    val id: Long,
    val requestId: Long,
    val proposedTeamId: Long,
    val proposedTeamName: String,
    val proposedBy: Long,
    val matchScore: Double?,
    val proposedDate: LocalDate?,
    val proposedTime: LocalTime?,
    val proposedGroundId: Long?,
    val memo: String?,
    val status: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(proposal: MatchingProposal): MatchingProposalResponse = MatchingProposalResponse(
            id = proposal.id,
            requestId = proposal.request.id,
            proposedTeamId = proposal.proposedTeam.id,
            proposedTeamName = proposal.proposedTeam.name,
            proposedBy = proposal.proposedBy,
            matchScore = proposal.matchScore,
            proposedDate = proposal.proposedDate,
            proposedTime = proposal.proposedTime,
            proposedGroundId = proposal.proposedGroundId,
            memo = proposal.memo,
            status = proposal.status.name,
            createdAt = proposal.createdAt,
        )
    }
}
