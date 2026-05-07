package com.dugout.api.domain.fee.repository

import com.dugout.api.domain.fee.entity.Fee
import org.springframework.data.jpa.repository.JpaRepository

interface FeeRepository : JpaRepository<Fee, Long> {
    fun findByTeamIdOrderByDueDateDesc(teamId: Long): List<Fee>
}
