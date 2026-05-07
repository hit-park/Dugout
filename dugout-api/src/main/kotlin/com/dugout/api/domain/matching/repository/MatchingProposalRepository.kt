package com.dugout.api.domain.matching.repository

import com.dugout.api.domain.matching.entity.MatchingProposal
import com.dugout.api.domain.matching.entity.MatchingProposalStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MatchingProposalRepository : JpaRepository<MatchingProposal, Long> {
    fun findByRequestIdOrderByCreatedAtAsc(requestId: Long): List<MatchingProposal>
    fun existsByRequestIdAndProposedTeamId(requestId: Long, proposedTeamId: Long): Boolean
    fun findByRequestIdAndStatus(
        requestId: Long,
        status: MatchingProposalStatus,
    ): List<MatchingProposal>
}
