package com.dugout.api.domain.team.repository

import com.dugout.api.domain.team.entity.Team
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRepository : JpaRepository<Team, Long> {
    fun findByInviteCode(inviteCode: String): Team?
    fun existsByInviteCode(inviteCode: String): Boolean
}
