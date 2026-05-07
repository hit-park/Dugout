package com.dugout.api.domain.lineup.repository

import com.dugout.api.domain.lineup.entity.Lineup
import org.springframework.data.jpa.repository.JpaRepository

interface LineupRepository : JpaRepository<Lineup, Long> {
    fun findByMatchId(matchId: Long): Lineup?
}
