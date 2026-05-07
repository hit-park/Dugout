package com.dugout.api.domain.mercenary.repository

import com.dugout.api.domain.mercenary.entity.MercenaryRequest
import com.dugout.api.domain.mercenary.entity.MercenaryRequestStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MercenaryRequestRepository : JpaRepository<MercenaryRequest, Long> {
    fun findByStatusOrderByCreatedAtDesc(status: MercenaryRequestStatus): List<MercenaryRequest>
    fun findByTeamIdOrderByCreatedAtDesc(teamId: Long): List<MercenaryRequest>
}
