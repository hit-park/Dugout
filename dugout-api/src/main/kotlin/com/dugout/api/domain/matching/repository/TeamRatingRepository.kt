package com.dugout.api.domain.matching.repository

import com.dugout.api.domain.matching.entity.TeamRating
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRatingRepository : JpaRepository<TeamRating, Long> {
    fun findByTeamId(teamId: Long): TeamRating?
    fun findAllByTeamIdIn(teamIds: Collection<Long>): List<TeamRating>
}
