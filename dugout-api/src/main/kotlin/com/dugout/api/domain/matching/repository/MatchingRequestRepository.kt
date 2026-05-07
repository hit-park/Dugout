package com.dugout.api.domain.matching.repository

import com.dugout.api.domain.matching.entity.MatchingRequest
import com.dugout.api.domain.matching.entity.MatchingRequestStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MatchingRequestRepository : JpaRepository<MatchingRequest, Long> {
    fun findByStatusOrderByCreatedAtDesc(status: MatchingRequestStatus): List<MatchingRequest>
    fun findByTeamIdOrderByCreatedAtDesc(teamId: Long): List<MatchingRequest>
}
