package com.dugout.api.domain.lineup.repository

import com.dugout.api.domain.lineup.entity.LineupEntry
import org.springframework.data.jpa.repository.JpaRepository

interface LineupEntryRepository : JpaRepository<LineupEntry, Long> {
    fun findByLineupIdOrderByBattingOrderAsc(lineupId: Long): List<LineupEntry>
    fun deleteByLineupId(lineupId: Long)
}
