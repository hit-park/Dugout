package com.dugout.api.domain.record.service

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.dto.PlateAppearanceResponse
import com.dugout.api.domain.record.entity.PlateAppearance
import com.dugout.api.domain.record.repository.PlateAppearanceRepository
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RecordService(
    private val plateAppearanceRepository: PlateAppearanceRepository,
    private val matchRepository: MatchRepository,
    private val teamMemberRepository: TeamMemberRepository,
) {

    @Transactional
    fun create(userId: Long, request: CreatePlateAppearanceRequest): PlateAppearanceResponse {
        val match = findMatch(request.matchId)
        requireTeamMember(match.team.id, userId)
        val teamMember = teamMemberRepository.findById(request.teamMemberId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND) }
        if (teamMember.team.id != match.team.id) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }

        val saved = plateAppearanceRepository.save(
            PlateAppearance(
                match = match,
                teamMember = teamMember,
                result = request.result,
                rbi = request.rbi,
            ),
        )
        return PlateAppearanceResponse.of(saved)
    }

    fun listByMatch(userId: Long, matchId: Long): List<PlateAppearanceResponse> {
        val match = findMatch(matchId)
        requireTeamMember(match.team.id, userId)
        return plateAppearanceRepository.findByMatchId(matchId).map(PlateAppearanceResponse::of)
    }

    @Transactional
    fun delete(userId: Long, recordId: Long) {
        val pa = plateAppearanceRepository.findById(recordId)
            .orElseThrow { BusinessException(ErrorCode.RECORD_NOT_FOUND) }
        requireTeamMember(pa.match.team.id, userId)
        plateAppearanceRepository.delete(pa)
    }

    private fun findMatch(matchId: Long): Match =
        matchRepository.findById(matchId)
            .orElseThrow { BusinessException(ErrorCode.MATCH_NOT_FOUND) }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId)) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
    }
}
