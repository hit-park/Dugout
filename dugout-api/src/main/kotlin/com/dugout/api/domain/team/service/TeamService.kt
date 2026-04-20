package com.dugout.api.domain.team.service

import com.dugout.api.domain.team.dto.CreateTeamRequest
import com.dugout.api.domain.team.dto.InviteCodeResponse
import com.dugout.api.domain.team.dto.JoinTeamRequest
import com.dugout.api.domain.team.dto.MyTeamResponse
import com.dugout.api.domain.team.dto.TeamMemberResponse
import com.dugout.api.domain.team.dto.TeamResponse
import com.dugout.api.domain.team.dto.UpdateMemberRequest
import com.dugout.api.domain.team.dto.UpdateTeamRequest
import com.dugout.api.domain.team.entity.LineupMode
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
@Transactional(readOnly = true)
class TeamService(
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val userRepository: UserRepository,
) {

    companion object {
        private const val INVITE_CODE_LENGTH = 8
        private val INVITE_CODE_ALPHABET = ('A'..'Z') + ('0'..'9')
        private const val INVITE_CODE_MAX_RETRY = 5
    }

    @Transactional
    fun createTeam(userId: Long, request: CreateTeamRequest): TeamResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val team = teamRepository.save(
            Team.create(
                name = request.name,
                region = request.region,
                division = request.division ?: 4,
                logoUrl = request.logoUrl,
                activityDays = request.activityDays ?: emptyList(),
                activityTime = request.activityTime,
                lineupMode = request.lineupMode?.let { parseLineupMode(it) } ?: LineupMode.BALANCED,
            ),
        )

        teamMemberRepository.save(
            TeamMember.create(team = team, user = user, role = TeamRole.CAPTAIN),
        )

        return TeamResponse.from(team, memberCount = 1)
    }

    fun getTeam(teamId: Long): TeamResponse {
        val team = findTeam(teamId)
        val memberCount = teamMemberRepository.countByTeamIdAndIsActiveTrue(teamId)
        return TeamResponse.from(team, memberCount)
    }

    @Transactional
    fun updateTeam(userId: Long, teamId: Long, request: UpdateTeamRequest): TeamResponse {
        requireTeamRole(teamId, userId, TeamRole.CAPTAIN, TeamRole.MANAGER)
        val team = findTeam(teamId)

        team.update(
            name = request.name,
            logoUrl = request.logoUrl,
            region = request.region,
            division = request.division,
            activityDays = request.activityDays,
            activityTime = request.activityTime,
            lineupMode = request.lineupMode?.let { parseLineupMode(it) },
        )

        val memberCount = teamMemberRepository.countByTeamIdAndIsActiveTrue(teamId)
        return TeamResponse.from(team, memberCount)
    }

    @Transactional
    fun generateInviteCode(userId: Long, teamId: Long): InviteCodeResponse {
        requireTeamRole(teamId, userId, TeamRole.CAPTAIN, TeamRole.MANAGER)
        val team = findTeam(teamId)

        val code = generateUniqueInviteCode()
        team.updateInviteCode(code)

        return InviteCodeResponse(code)
    }

    @Transactional
    fun joinTeam(userId: Long, request: JoinTeamRequest): TeamMemberResponse {
        val team = teamRepository.findByInviteCode(request.inviteCode)
            ?: throw BusinessException(ErrorCode.INVALID_INVITE_CODE)

        if (teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.id, userId)) {
            throw BusinessException(ErrorCode.ALREADY_TEAM_MEMBER)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val savedMember = teamMemberRepository.save(
            TeamMember.create(team = team, user = user, role = TeamRole.MEMBER),
        )

        return TeamMemberResponse.from(savedMember)
    }

    fun getMembers(teamId: Long): List<TeamMemberResponse> {
        findTeam(teamId) // 존재 검증
        return teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)
            .map(TeamMemberResponse::from)
    }

    @Transactional
    fun updateMember(
        userId: Long,
        teamId: Long,
        memberId: Long,
        request: UpdateMemberRequest,
    ): TeamMemberResponse {
        requireTeamRole(teamId, userId, TeamRole.CAPTAIN)

        val member = teamMemberRepository.findById(memberId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND) }

        if (member.team.id != teamId) {
            throw BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND)
        }

        request.role?.let { member.updateRole(parseTeamRole(it)) }
        member.updateInfo(request.jerseyNumber, request.positions)

        return TeamMemberResponse.from(member)
    }

    @Transactional
    fun removeMember(userId: Long, teamId: Long, memberId: Long) {
        requireTeamRole(teamId, userId, TeamRole.CAPTAIN)

        val member = teamMemberRepository.findById(memberId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND) }

        if (member.team.id != teamId) {
            throw BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND)
        }

        if (member.role == TeamRole.CAPTAIN) {
            throw BusinessException(ErrorCode.CAPTAIN_CANNOT_LEAVE)
        }

        member.deactivate()
    }

    fun getMyTeams(userId: Long): List<MyTeamResponse> =
        teamMemberRepository.findByUserIdAndIsActiveTrue(userId)
            .map(MyTeamResponse::from)

    private fun findTeam(teamId: Long): Team =
        teamRepository.findById(teamId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }

    private fun requireTeamRole(teamId: Long, userId: Long, vararg allowedRoles: TeamRole): TeamMember {
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)

        if (!member.isActive) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }

        if (member.role !in allowedRoles) {
            throw BusinessException(ErrorCode.TEAM_ROLE_NOT_ALLOWED)
        }

        return member
    }

    private fun generateUniqueInviteCode(): String {
        repeat(INVITE_CODE_MAX_RETRY) {
            val code = (1..INVITE_CODE_LENGTH)
                .map { INVITE_CODE_ALPHABET.random(Random.Default) }
                .joinToString("")
            if (!teamRepository.existsByInviteCode(code)) {
                return code
            }
        }
        throw BusinessException(ErrorCode.INTERNAL_ERROR)
    }

    private fun parseLineupMode(value: String): LineupMode = try {
        LineupMode.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        throw BusinessException(ErrorCode.INVALID_INPUT)
    }

    private fun parseTeamRole(value: String): TeamRole = try {
        TeamRole.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        throw BusinessException(ErrorCode.INVALID_INPUT)
    }
}
