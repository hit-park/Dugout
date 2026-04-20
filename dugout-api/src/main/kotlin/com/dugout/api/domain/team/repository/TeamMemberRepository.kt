package com.dugout.api.domain.team.repository

import com.dugout.api.domain.team.entity.TeamMember
import org.springframework.data.jpa.repository.JpaRepository

interface TeamMemberRepository : JpaRepository<TeamMember, Long> {
    fun findByTeamIdAndUserId(teamId: Long, userId: Long): TeamMember?
    fun findByTeamIdAndIsActiveTrue(teamId: Long): List<TeamMember>
    fun findByUserIdAndIsActiveTrue(userId: Long): List<TeamMember>
    fun existsByTeamIdAndUserIdAndIsActiveTrue(teamId: Long, userId: Long): Boolean
    fun countByTeamIdAndIsActiveTrue(teamId: Long): Int
}
