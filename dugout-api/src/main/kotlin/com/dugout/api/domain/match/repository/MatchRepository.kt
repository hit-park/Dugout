package com.dugout.api.domain.match.repository

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.entity.MatchStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MatchRepository : JpaRepository<Match, Long> {
    fun findByTeamIdOrderByMatchDateDesc(teamId: Long): List<Match>
    fun findByTeamIdAndMatchDateBetweenOrderByMatchDateAsc(
        teamId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<Match>

    fun findByStatusAndMatchDateBetween(
        status: MatchStatus,
        from: LocalDate,
        to: LocalDate,
    ): List<Match>
}
