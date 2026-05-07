package com.dugout.api.domain.fee.entity

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
import java.time.LocalDate

@Entity
@Table(name = "fees")
class Fee(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(nullable = false)
    var amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    var feeType: FeeType,

    @Column(name = "match_id")
    val matchId: Long? = null,

    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,

    @Column(columnDefinition = "TEXT")
    var memo: String? = null,
) : BaseEntity() {

    fun update(title: String?, amount: Long?, dueDate: LocalDate?, memo: String?) {
        title?.let { this.title = it }
        amount?.let { this.amount = it }
        dueDate?.let { this.dueDate = it }
        memo?.let { this.memo = it }
    }

    companion object {
        fun create(
            team: Team,
            title: String,
            amount: Long,
            feeType: FeeType,
            dueDate: LocalDate,
            matchId: Long? = null,
            memo: String? = null,
        ): Fee = Fee(
            team = team,
            title = title,
            amount = amount,
            feeType = feeType,
            matchId = matchId,
            dueDate = dueDate,
            memo = memo,
        )
    }
}
