package com.dugout.api.domain.lineup.entity

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "lineups",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["match_id"]),
    ],
)
class Lineup(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    val match: Match,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @Column(name = "is_ai_generated", nullable = false)
    var isAiGenerated: Boolean = false,

    @Column(name = "is_confirmed", nullable = false)
    var isConfirmed: Boolean = false,

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,
) : BaseEntity() {

    fun confirm(now: LocalDateTime = LocalDateTime.now()) {
        this.isConfirmed = true
        this.confirmedAt = now
    }

    fun markEdited() {
        this.isAiGenerated = false
    }

    companion object {
        fun create(
            match: Match,
            team: Team,
            createdBy: Long,
            isAiGenerated: Boolean = false,
        ): Lineup = Lineup(
            match = match,
            team = team,
            createdBy = createdBy,
            isAiGenerated = isAiGenerated,
        )
    }
}
