package com.dugout.api.domain.matching.entity

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(
    name = "matching_proposals",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["request_id", "proposed_team_id"]),
    ],
)
class MatchingProposal(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: MatchingRequest,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_team_id", nullable = false)
    val proposedTeam: Team,

    @Column(name = "proposed_by", nullable = false)
    val proposedBy: Long,

    @Column(name = "match_score")
    var matchScore: Double? = null,

    @Column(name = "proposed_date")
    var proposedDate: LocalDate? = null,

    @Column(name = "proposed_time")
    var proposedTime: LocalTime? = null,

    @Column(name = "proposed_ground_id")
    var proposedGroundId: Long? = null,

    @Column(columnDefinition = "TEXT")
    var memo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchingProposalStatus = MatchingProposalStatus.PENDING,
) : BaseEntity() {

    fun accept() {
        this.status = MatchingProposalStatus.ACCEPTED
    }

    fun reject() {
        this.status = MatchingProposalStatus.REJECTED
    }

    fun cancel() {
        this.status = MatchingProposalStatus.CANCELLED
    }

    fun isPending(): Boolean = status == MatchingProposalStatus.PENDING

    companion object {
        fun create(
            request: MatchingRequest,
            proposedTeam: Team,
            proposedBy: Long,
            proposedDate: LocalDate? = null,
            proposedTime: LocalTime? = null,
            proposedGroundId: Long? = null,
            memo: String? = null,
            matchScore: Double? = null,
        ): MatchingProposal = MatchingProposal(
            request = request,
            proposedTeam = proposedTeam,
            proposedBy = proposedBy,
            proposedDate = proposedDate,
            proposedTime = proposedTime,
            proposedGroundId = proposedGroundId,
            memo = memo,
            matchScore = matchScore,
        )
    }
}
