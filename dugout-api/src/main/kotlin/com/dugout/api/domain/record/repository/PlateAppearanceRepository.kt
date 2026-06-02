package com.dugout.api.domain.record.repository

import com.dugout.api.domain.record.entity.PlateAppearance
import org.springframework.data.jpa.repository.JpaRepository

interface PlateAppearanceRepository : JpaRepository<PlateAppearance, Long> {
    fun findByMatchId(matchId: Long): List<PlateAppearance>
    fun findByTeamMemberIdIn(teamMemberIds: Collection<Long>): List<PlateAppearance>
}
