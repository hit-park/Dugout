package com.dugout.api.domain.match.service

import com.dugout.api.domain.match.dto.CreateMatchRequest
import com.dugout.api.domain.match.dto.MatchResponse
import com.dugout.api.domain.match.dto.UpdateMatchRequest
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.entity.MatchStatus
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class MatchService(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
) {

    @Transactional
    fun createMatch(userId: Long, teamId: Long, request: CreateMatchRequest): MatchResponse {
        requireTeamManagement(teamId, userId)

        val team = teamRepository.findById(teamId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }

        val match = matchRepository.save(
            Match.create(
                team = team,
                matchDate = request.matchDate,
                matchTime = request.matchTime,
                gatherTime = request.gatherTime,
                opponentName = request.opponentName,
                opponentTeamId = request.opponentTeamId,
                groundId = request.groundId,
                groundName = request.groundName,
                voteDeadline = request.voteDeadline,
                memo = request.memo,
            ),
        )

        return MatchResponse.from(match)
    }

    fun getMatch(matchId: Long): MatchResponse {
        val match = findMatch(matchId)
        return MatchResponse.from(match)
    }

    fun getMatches(teamId: Long, from: LocalDate?, to: LocalDate?): List<MatchResponse> {
        requireTeamExists(teamId)

        val matches = if (from != null && to != null) {
            matchRepository.findByTeamIdAndMatchDateBetweenOrderByMatchDateAsc(teamId, from, to)
        } else {
            matchRepository.findByTeamIdOrderByMatchDateDesc(teamId)
        }

        return matches.map(MatchResponse::from)
    }

    @Transactional
    fun updateMatch(userId: Long, matchId: Long, request: UpdateMatchRequest): MatchResponse {
        val match = findMatch(matchId)
        requireTeamManagement(match.team.id, userId)

        if (match.status == MatchStatus.CANCELLED) {
            throw BusinessException(ErrorCode.MATCH_ALREADY_CANCELLED)
        }

        match.update(
            opponentName = request.opponentName,
            opponentTeamId = request.opponentTeamId,
            groundId = request.groundId,
            groundName = request.groundName,
            matchDate = request.matchDate,
            gatherTime = request.gatherTime,
            matchTime = request.matchTime,
            voteDeadline = request.voteDeadline,
            memo = request.memo,
        )

        return MatchResponse.from(match)
    }

    @Transactional
    fun cancelMatch(userId: Long, matchId: Long) {
        val match = findMatch(matchId)
        requireTeamManagement(match.team.id, userId)

        if (match.status == MatchStatus.CANCELLED) {
            throw BusinessException(ErrorCode.MATCH_ALREADY_CANCELLED)
        }

        match.cancel()
    }

    private fun findMatch(matchId: Long): Match =
        matchRepository.findById(matchId)
            .orElseThrow { BusinessException(ErrorCode.MATCH_NOT_FOUND) }

    private fun requireTeamExists(teamId: Long) {
        if (!teamRepository.existsById(teamId)) {
            throw BusinessException(ErrorCode.TEAM_NOT_FOUND)
        }
    }

    private fun requireTeamManagement(teamId: Long, userId: Long) {
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)

        if (!member.isActive) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }

        if (member.role !in listOf(TeamRole.CAPTAIN, TeamRole.MANAGER)) {
            throw BusinessException(ErrorCode.TEAM_ROLE_NOT_ALLOWED)
        }
    }
}
