package com.dugout.api.domain.fee.entity

import com.dugout.api.domain.user.entity.User
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
import java.time.LocalDateTime

@Entity
@Table(
    name = "fee_payments",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["fee_id", "user_id"]),
    ],
)
class FeePayment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = false)
    val fee: Fee,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "amount_paid", nullable = false)
    var amountPaid: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.UNPAID,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    @Column(name = "confirmed_by")
    var confirmedBy: Long? = null,

    @Column(columnDefinition = "TEXT")
    var memo: String? = null,
) : BaseEntity() {

    fun applyPayment(
        amountPaid: Long,
        status: PaymentStatus,
        confirmedBy: Long,
        memo: String? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        this.amountPaid = amountPaid
        this.status = status
        this.confirmedBy = confirmedBy
        this.memo = memo
        this.paidAt = if (status == PaymentStatus.UNPAID) null else now
    }

    companion object {
        fun ofUnpaid(fee: Fee, user: User): FeePayment = FeePayment(
            fee = fee,
            user = user,
            amountPaid = 0L,
            status = PaymentStatus.UNPAID,
        )
    }
}
