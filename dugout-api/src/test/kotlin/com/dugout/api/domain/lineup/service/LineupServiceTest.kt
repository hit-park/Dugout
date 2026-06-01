package com.dugout.api.domain.lineup.service

import com.dugout.api.domain.attendance.entity.Attendance
import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.lineup.dto.LineupEntryPayload
import com.dugout.api.domain.lineup.dto.SaveLineupRequest
import com.dugout.api.domain.lineup.entity.Lineup
import com.dugout.api.domain.lineup.entity.LineupEntry
import com.dugout.api.domain.lineup.repository.LineupEntryRepository
import com.dugout.api.domain.lineup.repository.LineupRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.ai.AiLineupAssignment
import com.dugout.api.global.ai.AiLineupRecommendResponse
import com.dugout.api.global.ai.DugoutAiClient
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LineupServiceTest {

    @Mock lateinit var lineupRepository: LineupRepository
    @Mock lateinit var lineupEntryRepository: LineupEntryRepository
    @Mock lateinit var matchRepository: MatchRepository
    @Mock lateinit var attendanceRepository: AttendanceRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository
    @Mock lateinit var dugoutAiClient: DugoutAiClient
    @Mock lateinit var applicationEventPublisher: ApplicationEventPublisher

    private lateinit var service: LineupService

    @BeforeEach
    fun setUp() {
        service = LineupService(
            lineupRepository, lineupEntryRepository, matchRepository,
            attendanceRepository, userRepository, teamMemberRepository, dugoutAiClient,
            applicationEventPublisher,
        )
    }

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")
    private fun sampleMatch(team: Team) = Match.create(
        team = team,
        matchDate = LocalDate.of(2026, 5, 20),
        matchTime = LocalTime.of(10, 0),
    )
    private fun user(nickname: String) = User.create(AuthProvider.KAKAO, "kakao-$nickname", nickname)

    @Test
    fun `추천 stub - 출석자가 9명 미만이면 INSUFFICIENT_ATTENDEES`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val attendees = (1..5).map {
            Attendance.create(match, user("p$it"), AttendanceStatus.ATTEND)
        }

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, 1L)).thenReturn(true)
        whenever(attendanceRepository.findByMatchIdOrderByRespondedAtAsc(match.id)).thenReturn(attendees)

        val exception = assertThrows<BusinessException> { service.recommend(1L, match.id) }
        assertEquals(ErrorCode.INSUFFICIENT_ATTENDEES, exception.errorCode)
    }

    @Test
    fun `추천 - AI 응답을 LineupRecommendationResponse로 매핑`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val attendees = (1..10).map {
            Attendance.create(match, user("p$it"), AttendanceStatus.ATTEND)
        }
        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, 1L)).thenReturn(true)
        whenever(attendanceRepository.findByMatchIdOrderByRespondedAtAsc(match.id)).thenReturn(attendees)
        whenever(dugoutAiClient.recommendLineup(any())).thenReturn(
            AiLineupRecommendResponse(
                matchId = match.id,
                isAiGenerated = true,
                source = "AI",
                entries = listOf(
                    AiLineupAssignment(userId = 100, position = "P", battingOrder = 1, isBench = false),
                    AiLineupAssignment(userId = 101, position = "C", battingOrder = 2, isBench = false),
                    AiLineupAssignment(userId = 102, position = "DH", battingOrder = null, isBench = true),
                ),
            ),
        )

        val response = service.recommend(1L, match.id)

        assertEquals("AI", response.source)
        assertEquals(true, response.isAiGenerated)
        assertEquals(3, response.entries.size)
        assertEquals(listOf("P", "C", "DH"), response.entries.map { it.position })
        assertTrue(response.entries.last().isBench)
    }

    @Test
    fun `라인업 저장 - 동일 사용자가 두 번 들어가면 INVALID_INPUT`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val captain = TeamMember.create(team, user("주장"), TeamRole.CAPTAIN)
        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        val exception = assertThrows<BusinessException> {
            service.saveLineup(
                1L, match.id,
                SaveLineupRequest(
                    entries = listOf(
                        LineupEntryPayload(userId = 10L, position = "P", battingOrder = 1),
                        LineupEntryPayload(userId = 10L, position = "C", battingOrder = 2),
                    ),
                ),
            )
        }
        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `라인업 저장 - 잘못된 포지션이면 INVALID_LINEUP_POSITION`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val captain = TeamMember.create(team, user("주장"), TeamRole.CAPTAIN)
        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        val exception = assertThrows<BusinessException> {
            service.saveLineup(
                1L, match.id,
                SaveLineupRequest(
                    entries = listOf(
                        LineupEntryPayload(userId = 10L, position = "QB", battingOrder = 1),
                    ),
                ),
            )
        }
        assertEquals(ErrorCode.INVALID_LINEUP_POSITION, exception.errorCode)
    }

    @Test
    fun `라인업 저장 - 이미 등록되어 있으면 LINEUP_ALREADY_EXISTS`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val captain = TeamMember.create(team, user("주장"), TeamRole.CAPTAIN)
        val existing = Lineup.create(match, team, createdBy = 1L)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)
        whenever(lineupRepository.findByMatchId(match.id)).thenReturn(existing)

        val exception = assertThrows<BusinessException> {
            service.saveLineup(
                1L, match.id,
                SaveLineupRequest(
                    entries = listOf(LineupEntryPayload(userId = 10L, position = "P", battingOrder = 1)),
                ),
            )
        }
        assertEquals(ErrorCode.LINEUP_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `라인업 수정 - 확정 상태면 LINEUP_ALREADY_CONFIRMED`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val captain = TeamMember.create(team, user("주장"), TeamRole.CAPTAIN)
        val confirmed = Lineup.create(match, team, createdBy = 1L).apply { confirm() }

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)
        whenever(lineupRepository.findByMatchId(match.id)).thenReturn(confirmed)

        val exception = assertThrows<BusinessException> {
            service.updateLineup(
                1L, match.id,
                SaveLineupRequest(
                    entries = listOf(LineupEntryPayload(userId = 10L, position = "P", battingOrder = 1)),
                ),
            )
        }
        assertEquals(ErrorCode.LINEUP_ALREADY_CONFIRMED, exception.errorCode)
    }

    @Test
    fun `라인업 확정 - 권한자가 호출하면 isConfirmed true가 됨`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val captain = TeamMember.create(team, user("주장"), TeamRole.CAPTAIN)
        val lineup = Lineup.create(match, team, createdBy = 1L)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)
        whenever(lineupRepository.findByMatchId(match.id)).thenReturn(lineup)
        whenever(lineupEntryRepository.findByLineupIdOrderByBattingOrderAsc(lineup.id))
            .thenReturn(emptyList<LineupEntry>())

        val response = service.confirmLineup(1L, match.id)

        assertEquals(true, response.isConfirmed)
        assertEquals(true, lineup.isConfirmed)
    }
}
