package com.dugout.api.domain.team.dto

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import java.time.LocalDateTime

data class TeamResponse(
    val id: Long,
    val name: String,
    val logoUrl: String?,
    val region: String,
    val division: Int,
    val homeGroundId: Long?,
    val activityDays: List<String>,
    val activityTime: String?,
    val inviteCode: String?,
    val lineupMode: String,
    val memberCount: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(team: Team, memberCount: Int): TeamResponse = TeamResponse(
            id = team.id,
            name = team.name,
            logoUrl = team.logoUrl,
            region = team.region,
            division = team.division,
            homeGroundId = team.homeGroundId,
            activityDays = team.activityDays,
            activityTime = team.activityTime,
            inviteCode = team.inviteCode,
            lineupMode = team.lineupMode.name,
            memberCount = memberCount,
            createdAt = team.createdAt,
        )
    }
}

data class TeamMemberResponse(
    val id: Long,
    val userId: Long,
    val nickname: String,
    val profileImgUrl: String?,
    val role: String,
    val jerseyNumber: Int?,
    val positions: List<String>,
    val isActive: Boolean,
    val joinedAt: LocalDateTime,
) {
    companion object {
        fun from(member: TeamMember): TeamMemberResponse = TeamMemberResponse(
            id = member.id,
            userId = member.user.id,
            nickname = member.user.nickname,
            profileImgUrl = member.user.profileImgUrl,
            role = member.role.name,
            jerseyNumber = member.jerseyNumber,
            positions = member.positions,
            isActive = member.isActive,
            joinedAt = member.joinedAt,
        )
    }
}

data class InviteCodeResponse(
    val inviteCode: String,
)

data class MyTeamResponse(
    val teamId: Long,
    val teamName: String,
    val logoUrl: String?,
    val role: String,
    val joinedAt: LocalDateTime,
) {
    companion object {
        fun from(member: TeamMember): MyTeamResponse = MyTeamResponse(
            teamId = member.team.id,
            teamName = member.team.name,
            logoUrl = member.team.logoUrl,
            role = member.role.name,
            joinedAt = member.joinedAt,
        )
    }
}
