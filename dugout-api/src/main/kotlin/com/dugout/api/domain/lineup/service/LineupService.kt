package com.dugout.api.domain.lineup.service

import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.lineup.dto.LineupEntryPayload
import com.dugout.api.domain.lineup.dto.LineupRecommendationResponse
import com.dugout.api.domain.lineup.dto.LineupResponse
import com.dugout.api.domain.lineup.dto.SaveLineupRequest
import com.dugout.api.domain.lineup.entity.Lineup
import com.dugout.api.domain.lineup.entity.LineupEntry
import com.dugout.api.domain.lineup.entity.Position
import com.dugout.api.domain.lineup.repository.LineupEntryRepository
import com.dugout.api.domain.lineup.repository.LineupRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.notification.event.LineupConfirmedEvent
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.ai.AiAttendeeProfile
import com.dugout.api.global.ai.AiLineupRecommendRequest
import com.dugout.api.global.ai.DugoutAiClient
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LineupService(
    private val lineupRepository: LineupRepository,
    private val lineupEntryRepository: LineupEntryRepository,
    private val matchRepository: MatchRepository,
    private val attendanceRepository: AttendanceRepository,
    private val userRepository: UserRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val dugoutAiClient: DugoutAiClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    /**
     * dugout-ai 헝가리안 알고리즘 호출. AI 미가용 시 BusinessException(AI_SERVICE_UNAVAILABLE).
     */
    fun recommend(userId: Long, matchId: Long): LineupRecommendationResponse {
        val match = findMatch(matchId)
        requireTeamMember(match.team.id, userId)

        val attendees = attendanceRepository.findByMatchIdOrderByRespondedAtAsc(matchId)
            .filter { it.status == AttendanceStatus.ATTEND || it.status == AttendanceStatus.LATE }
            .map { it.user }

        if (attendees.size < Position.FIELD_POSITIONS.size) {
            throw BusinessException(ErrorCode.INSUFFICIENT_ATTENDEES)
        }

        val aiResponse = dugoutAiClient.recommendLineup(
            AiLineupRecommendRequest(
                matchId = matchId,
                attendees = attendees.map { user ->
                    AiAttendeeProfile(
                        userId = user.id,
                        primaryPosition = "DH",  // 출석자 프로필이 없을 때는 DH로 보내고 AI가 적합도로 재배정
                    )
                },
                lineupMode = match.team.lineupMode.name,
            ),
        )

        val entries = aiResponse.entries.map { entry ->
            LineupEntryPayload(
                userId = entry.userId,
                position = entry.position,
                battingOrder = entry.battingOrder,
                isBench = entry.isBench,
            )
        }

        return LineupRecommendationResponse(
            matchId = matchId,
            isAiGenerated = true,
            source = aiResponse.source,
            entries = entries,
        )
    }

    @Transactional
    fun saveLineup(userId: Long, matchId: Long, request: SaveLineupRequest): LineupResponse {
        val match = findMatch(matchId)
        requireTeamManagement(match.team.id, userId)
        validateEntries(request.entries)

        if (lineupRepository.findByMatchId(matchId) != null) {
            throw BusinessException(ErrorCode.LINEUP_ALREADY_EXISTS)
        }

        val lineup = lineupRepository.save(
            Lineup.create(
                match = match,
                team = match.team,
                createdBy = userId,
                isAiGenerated = false,
            ),
        )
        val savedEntries = persistEntries(lineup, request.entries)
        return LineupResponse.of(lineup, savedEntries)
    }

    @Transactional
    fun updateLineup(userId: Long, matchId: Long, request: SaveLineupRequest): LineupResponse {
        val match = findMatch(matchId)
        requireTeamManagement(match.team.id, userId)
        validateEntries(request.entries)

        val lineup = lineupRepository.findByMatchId(matchId)
            ?: throw BusinessException(ErrorCode.LINEUP_NOT_FOUND)
        if (lineup.isConfirmed) {
            throw BusinessException(ErrorCode.LINEUP_ALREADY_CONFIRMED)
        }

        lineupEntryRepository.deleteByLineupId(lineup.id)
        lineup.markEdited()
        val savedEntries = persistEntries(lineup, request.entries)
        return LineupResponse.of(lineup, savedEntries)
    }

    fun getLineup(userId: Long, matchId: Long): LineupResponse {
        val match = findMatch(matchId)
        requireTeamMember(match.team.id, userId)

        val lineup = lineupRepository.findByMatchId(matchId)
            ?: throw BusinessException(ErrorCode.LINEUP_NOT_FOUND)
        val entries = lineupEntryRepository.findByLineupIdOrderByBattingOrderAsc(lineup.id)
        return LineupResponse.of(lineup, entries)
    }

    @Transactional
    fun confirmLineup(userId: Long, matchId: Long): LineupResponse {
        val match = findMatch(matchId)
        requireTeamManagement(match.team.id, userId)

        val lineup = lineupRepository.findByMatchId(matchId)
            ?: throw BusinessException(ErrorCode.LINEUP_NOT_FOUND)
        if (!lineup.isConfirmed) {
            lineup.confirm()
        }
        val entries = lineupEntryRepository.findByLineupIdOrderByBattingOrderAsc(lineup.id)

        applicationEventPublisher.publishEvent(
            LineupConfirmedEvent(
                lineupId = lineup.id,
                matchId = lineup.match.id,
                teamId = match.team.id,
                confirmedBy = userId,
            ),
        )

        return LineupResponse.of(lineup, entries)
    }

    /**
     * Phase 1 카드 이미지 stub — 실제 S3 업로드는 후속 작업. 라인업 데이터를 그대로 응답.
     */
    fun getLineupCard(userId: Long, matchId: Long): LineupResponse = getLineup(userId, matchId)

    private fun persistEntries(
        lineup: Lineup,
        payloads: List<LineupEntryPayload>,
    ): List<LineupEntry> {
        val users = loadUsers(payloads.map { it.userId })
        val entries = payloads.map { payload ->
            LineupEntry(
                lineup = lineup,
                user = users.getValue(payload.userId),
                position = payload.position,
                battingOrder = payload.battingOrder,
                isBench = payload.isBench,
            )
        }
        return lineupEntryRepository.saveAll(entries)
    }

    private fun loadUsers(userIds: List<Long>): Map<Long, User> {
        val users = userRepository.findAllById(userIds)
        if (users.size != userIds.toSet().size) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        return users.associateBy { it.id }
    }

    private fun validateEntries(entries: List<LineupEntryPayload>) {
        if (entries.isEmpty()) {
            throw BusinessException(ErrorCode.INSUFFICIENT_ATTENDEES)
        }
        entries.forEach { entry ->
            if (!Position.isValid(entry.position)) {
                throw BusinessException(ErrorCode.INVALID_LINEUP_POSITION)
            }
        }
        val userIds = entries.map { it.userId }
        if (userIds.size != userIds.toSet().size) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
        val battingOrders = entries.mapNotNull { it.battingOrder }
        if (battingOrders.size != battingOrders.toSet().size) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun findMatch(matchId: Long): Match =
        matchRepository.findById(matchId)
            .orElseThrow { BusinessException(ErrorCode.MATCH_NOT_FOUND) }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId)) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
    }

    private fun requireTeamManagement(teamId: Long, userId: Long) {
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        if (!member.isActive) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
        if (member.role !in MANAGEMENT_ROLES) {
            throw BusinessException(ErrorCode.TEAM_ROLE_NOT_ALLOWED)
        }
    }

    companion object {
        private val MANAGEMENT_ROLES = listOf(TeamRole.CAPTAIN, TeamRole.MANAGER)
    }
}
