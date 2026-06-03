package com.dugout.api.domain.record.entity

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "plate_appearances")
class PlateAppearance(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    val match: Match,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id", nullable = false)
    val teamMember: TeamMember,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val result: BattingResult,

    @Column(nullable = false)
    val rbi: Int = 0,
) : BaseEntity()
