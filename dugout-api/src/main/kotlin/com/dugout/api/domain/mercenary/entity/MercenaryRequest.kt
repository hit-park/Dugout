package com.dugout.api.domain.mercenary.entity

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.common.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "mercenary_requests")
class MercenaryRequest(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @Column(name = "match_id", nullable = false)
    val matchId: Long,

    @Convert(converter = StringListConverter::class)
    @Column(name = "needed_positions", length = 200, nullable = false)
    var neededPositions: List<String> = emptyList(),

    @Column(name = "needed_count", nullable = false)
    var neededCount: Int,

    @Column(name = "skill_min")
    var skillMin: Int? = null,

    @Column(name = "skill_max")
    var skillMax: Int? = null,

    @Column
    var fee: Long? = null,

    @Convert(converter = StringListConverter::class)
    @Column(length = 200, nullable = false)
    var regions: List<String> = emptyList(),

    @Column(columnDefinition = "TEXT")
    var memo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MercenaryRequestStatus = MercenaryRequestStatus.OPEN,
) : BaseEntity() {

    fun close() {
        this.status = MercenaryRequestStatus.CLOSED
    }

    fun cancel() {
        this.status = MercenaryRequestStatus.CANCELLED
    }

    fun isOpen(): Boolean = status == MercenaryRequestStatus.OPEN

    companion object {
        fun create(
            team: Team,
            matchId: Long,
            neededPositions: List<String>,
            neededCount: Int,
            skillMin: Int? = null,
            skillMax: Int? = null,
            fee: Long? = null,
            regions: List<String> = emptyList(),
            memo: String? = null,
        ): MercenaryRequest = MercenaryRequest(
            team = team,
            matchId = matchId,
            neededPositions = neededPositions,
            neededCount = neededCount,
            skillMin = skillMin,
            skillMax = skillMax,
            fee = fee,
            regions = regions,
            memo = memo,
        )
    }
}
