package com.dugout.api.domain.record.service

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.record.dto.BattingStatsResponse
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.dto.PlateAppearanceResponse
import com.dugout.api.domain.record.entity.BattingResult
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

    fun battingStats(userId: Long, teamId: Long): List<BattingStatsResponse> {
        requireTeamMember(teamId, userId)
        val memberIds = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId).map { it.id }
        if (memberIds.isEmpty()) return emptyList()

        return plateAppearanceRepository.findByTeamMemberIdIn(memberIds)
            .groupBy { it.teamMember.id }
            .map { (memberId, list) -> aggregate(memberId, list) }
    }

    private fun aggregate(memberId: Long, list: List<PlateAppearance>): BattingStatsResponse {
        fun count(r: BattingResult) = list.count { it.result == r }
        val singles = count(BattingResult.SINGLE)
        val doubles = count(BattingResult.DOUBLE)
        val triples = count(BattingResult.TRIPLE)
        val hr = count(BattingResult.HOME_RUN)
        val bb = count(BattingResult.WALK)
        val hbp = count(BattingResult.HIT_BY_PITCH)
        val sf = count(BattingResult.SACRIFICE_FLY)

        val pa = list.size
        val hits = singles + doubles + triples + hr
        val ab = pa - bb - hbp - sf
        val totalBases = singles + 2 * doubles + 3 * triples + 4 * hr
        val obpDenom = ab + bb + hbp + sf

        fun ratio(num: Int, den: Int) = if (den == 0) 0.0 else num.toDouble() / den
        val avg = ratio(hits, ab)
        val obp = ratio(hits + bb + hbp, obpDenom)
        val slg = ratio(totalBases, ab)

        return BattingStatsResponse(
            teamMemberId = memberId,
            plateAppearances = pa,
            atBats = ab,
            hits = hits,
            avg = avg,
            obp = obp,
            slg = slg,
            ops = obp + slg,
        )
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
