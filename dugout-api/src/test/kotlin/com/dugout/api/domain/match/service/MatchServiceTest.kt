package com.dugout.api.domain.match.service

import com.dugout.api.domain.match.dto.CreateMatchRequest
import com.dugout.api.domain.match.dto.UpdateMatchRequest
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.entity.MatchStatus
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MatchServiceTest {

    @Mock lateinit var matchRepository: MatchRepository
    @Mock lateinit var teamRepository: TeamRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository

    private lateinit var matchService: MatchService

    @BeforeEach
    fun setUp() {
        matchService = MatchService(matchRepository, teamRepository, teamMemberRepository)
    }

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")
    private fun sampleUser() = User.create(AuthProvider.KAKAO, "kakao-1", "김주장")

    @Test
    fun `경기 생성 - CAPTAIN이 생성 가능`() {
        val team = sampleTeam()
        val user = sampleUser()
        val captainMember = TeamMember.create(team, user, TeamRole.CAPTAIN)

        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captainMember)
        whenever(teamRepository.findById(team.id)).thenReturn(Optional.of(team))
        whenever(matchRepository.save(any<Match>())).thenAnswer { it.getArgument<Match>(0) }

        val response = matchService.createMatch(
            userId = 1L,
            teamId = team.id,
            request = CreateMatchRequest(
                matchDate = LocalDate.of(2026, 5, 1),
                matchTime = LocalTime.of(10, 0),
                opponentName = "파이어볼즈",
            ),
        )

        assertEquals("SCHEDULED", response.status)
        assertEquals("파이어볼즈", response.opponentName)
    }

    @Test
    fun `경기 생성 - MEMBER는 TEAM_ROLE_NOT_ALLOWED`() {
        val team = sampleTeam()
        val user = sampleUser()
        val member = TeamMember.create(team, user, TeamRole.MEMBER)

        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(member)

        val exception = assertThrows<BusinessException> {
            matchService.createMatch(
                userId = 1L,
                teamId = team.id,
                request = CreateMatchRequest(
                    matchDate = LocalDate.of(2026, 5, 1),
                    matchTime = LocalTime.of(10, 0),
                ),
            )
        }
        assertEquals(ErrorCode.TEAM_ROLE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `경기 생성 - 팀 멤버가 아니면 NOT_TEAM_MEMBER`() {
        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            matchService.createMatch(
                1L, 1L,
                CreateMatchRequest(LocalDate.of(2026, 5, 1), LocalTime.of(10, 0)),
            )
        }
        assertEquals(ErrorCode.NOT_TEAM_MEMBER, exception.errorCode)
    }

    @Test
    fun `경기 조회 - 존재하지 않으면 MATCH_NOT_FOUND`() {
        whenever(matchRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessException> { matchService.getMatch(999L) }
        assertEquals(ErrorCode.MATCH_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `경기 수정 - CAPTAIN이 수정 가능`() {
        val team = sampleTeam()
        val user = sampleUser()
        val captain = TeamMember.create(team, user, TeamRole.CAPTAIN)
        val match = Match.create(team, LocalDate.of(2026, 5, 1), LocalTime.of(10, 0))

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        val response = matchService.updateMatch(
            userId = 1L,
            matchId = match.id,
            request = UpdateMatchRequest(memo = "장비 챙겨오기"),
        )

        assertEquals("장비 챙겨오기", response.memo)
    }

    @Test
    fun `경기 수정 - 취소된 경기면 MATCH_ALREADY_CANCELLED`() {
        val team = sampleTeam()
        val user = sampleUser()
        val captain = TeamMember.create(team, user, TeamRole.CAPTAIN)
        val match = Match.create(team, LocalDate.of(2026, 5, 1), LocalTime.of(10, 0))
        match.cancel()

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        val exception = assertThrows<BusinessException> {
            matchService.updateMatch(1L, match.id, UpdateMatchRequest(memo = "변경"))
        }
        assertEquals(ErrorCode.MATCH_ALREADY_CANCELLED, exception.errorCode)
    }

    @Test
    fun `경기 취소 - CAPTAIN이 취소 가능`() {
        val team = sampleTeam()
        val user = sampleUser()
        val captain = TeamMember.create(team, user, TeamRole.CAPTAIN)
        val match = Match.create(team, LocalDate.of(2026, 5, 1), LocalTime.of(10, 0))

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)

        matchService.cancelMatch(1L, match.id)

        assertEquals(MatchStatus.CANCELLED, match.status)
    }
}
