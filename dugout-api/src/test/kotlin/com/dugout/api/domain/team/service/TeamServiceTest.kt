package com.dugout.api.domain.team.service

import com.dugout.api.domain.team.dto.CreateTeamRequest
import com.dugout.api.domain.team.dto.JoinTeamRequest
import com.dugout.api.domain.team.dto.UpdateMemberRequest
import com.dugout.api.domain.team.dto.UpdateTeamRequest
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
class TeamServiceTest {

    @Mock lateinit var teamRepository: TeamRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository
    @Mock lateinit var userRepository: UserRepository

    private lateinit var teamService: TeamService

    @BeforeEach
    fun setUp() {
        teamService = TeamService(teamRepository, teamMemberRepository, userRepository)
    }

    private fun sampleUser() = User.create(
        provider = AuthProvider.KAKAO,
        providerId = "kakao-1",
        nickname = "김주장",
    )

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")

    @Test
    fun `팀 생성 - 생성자가 CAPTAIN으로 등록됨`() {
        val user = sampleUser()
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(teamRepository.save(any<Team>())).thenAnswer { it.getArgument<Team>(0) }
        whenever(teamMemberRepository.save(any<TeamMember>())).thenAnswer { it.getArgument<TeamMember>(0) }

        val response = teamService.createTeam(
            userId = 1L,
            request = CreateTeamRequest(name = "두갓FC", region = "서울 강남구"),
        )

        assertEquals("두갓FC", response.name)
        assertEquals("서울 강남구", response.region)
        assertEquals(4, response.division)
        assertEquals(1, response.memberCount)
        assertEquals("BALANCED", response.lineupMode)
    }

    @Test
    fun `팀 조회 - 존재하지 않는 팀이면 TEAM_NOT_FOUND`() {
        whenever(teamRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessException> { teamService.getTeam(999L) }
        assertEquals(ErrorCode.TEAM_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `팀 수정 - CAPTAIN은 수정 가능`() {
        val user = sampleUser()
        val team = sampleTeam()
        val captain = TeamMember.create(team, user, TeamRole.CAPTAIN)

        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(captain)
        whenever(teamRepository.findById(1L)).thenReturn(Optional.of(team))
        whenever(teamMemberRepository.countByTeamIdAndIsActiveTrue(1L)).thenReturn(1)

        val response = teamService.updateTeam(
            userId = 1L,
            teamId = 1L,
            request = UpdateTeamRequest(name = "새팀명"),
        )

        assertEquals("새팀명", response.name)
    }

    @Test
    fun `팀 수정 - MEMBER는 수정 시 TEAM_ROLE_NOT_ALLOWED`() {
        val user = sampleUser()
        val team = sampleTeam()
        val member = TeamMember.create(team, user, TeamRole.MEMBER)

        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(member)

        val exception = assertThrows<BusinessException> {
            teamService.updateTeam(1L, 1L, UpdateTeamRequest(name = "시도"))
        }
        assertEquals(ErrorCode.TEAM_ROLE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `팀 수정 - 팀 멤버가 아니면 NOT_TEAM_MEMBER`() {
        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            teamService.updateTeam(1L, 1L, UpdateTeamRequest(name = "시도"))
        }
        assertEquals(ErrorCode.NOT_TEAM_MEMBER, exception.errorCode)
    }

    @Test
    fun `초대 코드 생성 - 8자리 코드 반환`() {
        val user = sampleUser()
        val team = sampleTeam()
        val captain = TeamMember.create(team, user, TeamRole.CAPTAIN)

        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(captain)
        whenever(teamRepository.findById(1L)).thenReturn(Optional.of(team))
        whenever(teamRepository.existsByInviteCode(any())).thenReturn(false)

        val response = teamService.generateInviteCode(1L, 1L)

        assertEquals(8, response.inviteCode.length)
        assertTrue(response.inviteCode.all { it.isUpperCase() || it.isDigit() })
    }

    @Test
    fun `팀 가입 - MEMBER로 가입 성공`() {
        val team = sampleTeam().apply { updateInviteCode("ABCD1234") }
        val user = sampleUser()

        whenever(teamRepository.findByInviteCode("ABCD1234")).thenReturn(team)
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, 2L)).thenReturn(false)
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(user))
        whenever(teamMemberRepository.save(any<TeamMember>())).thenAnswer { it.getArgument<TeamMember>(0) }

        val response = teamService.joinTeam(2L, JoinTeamRequest("ABCD1234"))

        assertEquals("MEMBER", response.role)
    }

    @Test
    fun `팀 가입 - 잘못된 초대 코드면 INVALID_INVITE_CODE`() {
        whenever(teamRepository.findByInviteCode("BAD")).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            teamService.joinTeam(1L, JoinTeamRequest("BAD"))
        }
        assertEquals(ErrorCode.INVALID_INVITE_CODE, exception.errorCode)
    }

    @Test
    fun `팀 가입 - 이미 멤버면 ALREADY_TEAM_MEMBER`() {
        val team = sampleTeam().apply { updateInviteCode("ABCD1234") }
        whenever(teamRepository.findByInviteCode("ABCD1234")).thenReturn(team)
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, 1L)).thenReturn(true)

        val exception = assertThrows<BusinessException> {
            teamService.joinTeam(1L, JoinTeamRequest("ABCD1234"))
        }
        assertEquals(ErrorCode.ALREADY_TEAM_MEMBER, exception.errorCode)
    }

    @Test
    fun `멤버 역할 변경 - CAPTAIN만 가능`() {
        val captain = sampleUser()
        val target = User.create(AuthProvider.KAKAO, "kakao-2", "박선수")
        val team = sampleTeam()
        val captainMember = TeamMember.create(team, captain, TeamRole.CAPTAIN)
        val targetMember = TeamMember.create(team, target, TeamRole.MEMBER)

        // team.id는 BaseEntity 기본값 0 (DB 저장 전)
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captainMember)
        whenever(teamMemberRepository.findById(10L)).thenReturn(Optional.of(targetMember))

        val response = teamService.updateMember(
            userId = 1L,
            teamId = team.id,
            memberId = 10L,
            request = UpdateMemberRequest(role = "MANAGER"),
        )

        assertEquals("MANAGER", response.role)
    }

    @Test
    fun `멤버 역할 변경 - MANAGER가 시도하면 TEAM_ROLE_NOT_ALLOWED`() {
        val user = sampleUser()
        val team = sampleTeam()
        val managerMember = TeamMember.create(team, user, TeamRole.MANAGER)

        whenever(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(managerMember)

        val exception = assertThrows<BusinessException> {
            teamService.updateMember(1L, 1L, 10L, UpdateMemberRequest(role = "CAPTAIN"))
        }
        assertEquals(ErrorCode.TEAM_ROLE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `멤버 제거 - CAPTAIN은 다른 멤버를 제거할 수 있음`() {
        val captain = sampleUser()
        val target = User.create(AuthProvider.KAKAO, "kakao-2", "박선수")
        val team = sampleTeam()
        val captainMember = TeamMember.create(team, captain, TeamRole.CAPTAIN)
        val targetMember = TeamMember.create(team, target, TeamRole.MEMBER)

        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captainMember)
        whenever(teamMemberRepository.findById(10L)).thenReturn(Optional.of(targetMember))

        teamService.removeMember(1L, team.id, 10L)

        assertTrue(!targetMember.isActive)
    }

    @Test
    fun `멤버 제거 - CAPTAIN 본인은 제거 불가 (CAPTAIN_CANNOT_LEAVE)`() {
        val captain = sampleUser()
        val team = sampleTeam()
        val captainMember = TeamMember.create(team, captain, TeamRole.CAPTAIN)

        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(captainMember)
        whenever(teamMemberRepository.findById(10L)).thenReturn(Optional.of(captainMember))

        val exception = assertThrows<BusinessException> {
            teamService.removeMember(1L, team.id, 10L)
        }
        assertEquals(ErrorCode.CAPTAIN_CANNOT_LEAVE, exception.errorCode)
    }

    @Test
    fun `내 팀 목록 조회 - 활성 멤버십만 반환`() {
        val user = sampleUser()
        val team1 = Team.create(name = "팀A", region = "서울")
        val team2 = Team.create(name = "팀B", region = "부산")
        val m1 = TeamMember.create(team1, user, TeamRole.CAPTAIN)
        val m2 = TeamMember.create(team2, user, TeamRole.MEMBER)

        whenever(teamMemberRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(listOf(m1, m2))

        val response = teamService.getMyTeams(1L)

        assertEquals(2, response.size)
        assertNotNull(response.find { it.teamName == "팀A" })
        assertNotNull(response.find { it.teamName == "팀B" })
    }
}
