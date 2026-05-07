package com.dugout.api.domain.matching.service

import com.dugout.api.domain.matching.dto.CreateMatchingRequestPayload
import com.dugout.api.domain.matching.dto.MatchResultPayload
import com.dugout.api.domain.matching.dto.ProposeMatchPayload
import com.dugout.api.domain.matching.entity.MatchingProposal
import com.dugout.api.domain.matching.entity.MatchingRequest
import com.dugout.api.domain.matching.entity.TeamRating
import com.dugout.api.domain.matching.repository.MatchingProposalRepository
import com.dugout.api.domain.matching.repository.MatchingRequestRepository
import com.dugout.api.domain.matching.repository.TeamRatingRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MatchingServiceTest {

    @Mock lateinit var requestRepository: MatchingRequestRepository
    @Mock lateinit var proposalRepository: MatchingProposalRepository
    @Mock lateinit var teamRatingRepository: TeamRatingRepository
    @Mock lateinit var teamRepository: TeamRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository
    @Mock lateinit var dugoutAiClient: DugoutAiClient

    private lateinit var service: MatchingService

    @BeforeEach
    fun setUp() {
        service = MatchingService(
            requestRepository, proposalRepository, teamRatingRepository,
            teamRepository, teamMemberRepository, dugoutAiClient,
        )
    }

    private fun homeTeam() = Team.create(name = "홈팀", region = "서울", division = 3)
    private fun awayTeam() = Team.create(name = "원정팀", region = "서울", division = 3)
    private fun captainOf(team: Team) = TeamMember.create(
        team, User.create(AuthProvider.KAKAO, "kakao-c", "주장"), TeamRole.CAPTAIN,
    )

    @Test
    fun `매칭 요청 생성 - 권한 자가 호출하면 OPEN 상태로 저장`() {
        val team = homeTeam()
        val captain = captainOf(team)
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)
        whenever(teamRepository.findById(team.id)).thenReturn(Optional.of(team))
        whenever(teamRatingRepository.findByTeamId(team.id)).thenReturn(null)
        whenever(teamRatingRepository.save(any<TeamRating>())).thenAnswer {
            it.getArgument<TeamRating>(0)
        }
        whenever(requestRepository.save(any<MatchingRequest>())).thenAnswer {
            it.getArgument<MatchingRequest>(0)
        }

        val response = service.createRequest(
            1L, team.id,
            CreateMatchingRequestPayload(preferredDates = listOf("2026-05-30")),
        )

        assertEquals("OPEN", response.status)
        assertEquals(listOf("2026-05-30"), response.preferredDates)
    }

    @Test
    fun `매칭 제안 - 자기 팀에 제안 시 SELF_MATCHING_NOT_ALLOWED`() {
        val team = homeTeam()
        val captain = captainOf(team)
        val request = MatchingRequest.create(team = team, preferredDates = listOf("2026-05-30"))

        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captain)
        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))

        val exception = assertThrows<BusinessException> {
            service.proposeMatch(1L, request.id, team.id, ProposeMatchPayload())
        }
        assertEquals(ErrorCode.SELF_MATCHING_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `매칭 제안 - OPEN 아닌 요청에는 MATCHING_REQUEST_NOT_OPEN`() {
        val home = homeTeam()
        val away = awayTeam()
        val captain = captainOf(away)
        val request = MatchingRequest.create(team = home, preferredDates = listOf("2026-05-30"))
        request.cancel()

        whenever(teamMemberRepository.findByTeamIdAndUserId(away.id, 1L)).thenReturn(captain)
        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))

        val exception = assertThrows<BusinessException> {
            service.proposeMatch(1L, request.id, away.id, ProposeMatchPayload())
        }
        assertEquals(ErrorCode.MATCHING_REQUEST_NOT_OPEN, exception.errorCode)
    }

    @Test
    fun `결과 입력 - status COMPLETED 와 점수가 저장됨 (ELO 갱신 자체는 TeamRatingTest로 검증)`() {
        val home = homeTeam()
        val away = awayTeam()
        val captain = captainOf(home)
        val request = MatchingRequest.create(team = home, preferredDates = listOf("2026-05-30"))
        request.applyAcceptedProposal(
            opponentTeamId = away.id,
            matchDate = null,
            matchTime = null,
            groundId = null,
        )
        val homeRating = TeamRating(team = home, eloRating = 1200)
        val awayRating = TeamRating(team = away, eloRating = 1200)

        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))
        whenever(teamMemberRepository.findByTeamIdAndUserId(home.id, 1L)).thenReturn(captain)
        // BaseEntity.id가 둘 다 0L이라 분리 stub 불가 — any() 매처 + thenReturn 순차 응답
        whenever(teamRatingRepository.findByTeamId(any()))
            .thenReturn(homeRating)
            .thenReturn(awayRating)

        val response = service.applyResult(
            1L, request.id,
            MatchResultPayload(homeScore = 5, awayScore = 3),
        )

        assertEquals("COMPLETED", response.status)
        assertEquals(5, response.resultHomeScore)
        assertEquals(3, response.resultAwayScore)
        assertTrue(homeRating.wins == 1 || awayRating.wins == 1) // 두 객체 중 하나는 승리 카운트
    }

    @Test
    fun `결과 입력 - MATCHED 상태 아니면 MATCHING_REQUEST_NOT_OPEN`() {
        val home = homeTeam()
        val captain = captainOf(home)
        val request = MatchingRequest.create(team = home, preferredDates = listOf("2026-05-30"))

        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))
        whenever(teamMemberRepository.findByTeamIdAndUserId(home.id, 1L)).thenReturn(captain)

        val exception = assertThrows<BusinessException> {
            service.applyResult(1L, request.id, MatchResultPayload(5, 3))
        }
        assertEquals(ErrorCode.MATCHING_REQUEST_NOT_OPEN, exception.errorCode)
    }

    @Test
    fun `제안 수락 - 다른 PENDING 제안들이 자동 REJECTED`() {
        val home = homeTeam()
        val away1 = awayTeam()
        val away2 = Team.create(name = "원정2", region = "서울", division = 3)
        val captain = captainOf(home)
        val request = MatchingRequest.create(team = home, preferredDates = listOf("2026-05-30"))
        val accepted = MatchingProposal.create(request = request, proposedTeam = away1, proposedBy = 99L)
        val other = MatchingProposal.create(request = request, proposedTeam = away2, proposedBy = 100L)

        whenever(proposalRepository.findById(accepted.id)).thenReturn(Optional.of(accepted))
        whenever(teamMemberRepository.findByTeamIdAndUserId(home.id, 1L)).thenReturn(captain)
        whenever(proposalRepository.findByRequestIdAndStatus(any(), any()))
            .thenReturn(listOf(accepted, other))

        val response = service.acceptProposal(1L, accepted.id)

        assertEquals("ACCEPTED", response.status)
        // request 상태도 MATCHED
        assertEquals("MATCHED", com.dugout.api.domain.matching.dto.MatchingRequestResponse.from(request).status)
        // 다른 제안은 자동 REJECTED
        assertEquals(com.dugout.api.domain.matching.entity.MatchingProposalStatus.REJECTED, other.status)
    }
}
