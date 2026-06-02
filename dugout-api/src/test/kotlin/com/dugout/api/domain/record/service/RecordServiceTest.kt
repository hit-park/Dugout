package com.dugout.api.domain.record.service

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.entity.BattingResult
import com.dugout.api.domain.record.entity.PlateAppearance
import com.dugout.api.domain.record.repository.PlateAppearanceRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
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
class RecordServiceTest {

    @Mock lateinit var plateAppearanceRepository: PlateAppearanceRepository
    @Mock lateinit var matchRepository: MatchRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository

    private lateinit var service: RecordService

    @BeforeEach
    fun setUp() {
        service = RecordService(plateAppearanceRepository, matchRepository, teamMemberRepository)
    }

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")
    private fun sampleMatch(team: Team) = Match.create(
        team = team,
        matchDate = LocalDate.of(2026, 5, 20),
        matchTime = LocalTime.of(10, 0),
    )
    private fun sampleUser(nickname: String) = User.create(AuthProvider.KAKAO, "kakao-$nickname", nickname)
    private fun sampleMember(team: Team, user: User) = TeamMember.create(team, user, TeamRole.MEMBER)

    @Test
    fun `경기가 없으면 MATCH_NOT_FOUND`() {
        whenever(matchRepository.findById(1L)).thenReturn(Optional.empty())
        val req = CreatePlateAppearanceRequest(matchId = 1L, teamMemberId = 2L, result = BattingResult.SINGLE)

        val ex = assertThrows<BusinessException> { service.create(userId = 99L, request = req) }

        assertEquals(ErrorCode.MATCH_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `요청자가 팀 멤버가 아니면 NOT_TEAM_MEMBER`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, 99L)).thenReturn(false)

        val req = CreatePlateAppearanceRequest(matchId = match.id, teamMemberId = 2L, result = BattingResult.SINGLE)
        val ex = assertThrows<BusinessException> { service.create(userId = 99L, request = req) }

        assertEquals(ErrorCode.NOT_TEAM_MEMBER, ex.errorCode)
    }

    @Test
    fun `정상 생성 시 저장된 응답 반환`() {
        val team = sampleTeam()
        val match = sampleMatch(team)
        val user = sampleUser("선수")
        val member = sampleMember(team, user)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, 99L)).thenReturn(true)
        whenever(teamMemberRepository.findById(member.id)).thenReturn(Optional.of(member))
        whenever(plateAppearanceRepository.save(any<PlateAppearance>()))
            .thenAnswer { it.arguments[0] as PlateAppearance }

        val req = CreatePlateAppearanceRequest(matchId = match.id, teamMemberId = member.id, result = BattingResult.DOUBLE, rbi = 1)
        val res = service.create(userId = 99L, request = req)

        assertEquals(BattingResult.DOUBLE, res.result)
        assertEquals(1, res.rbi)
    }
}
