package com.dugout.api.domain.fee.repository

import com.dugout.api.domain.fee.entity.FeePayment
import com.dugout.api.domain.fee.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FeePaymentRepository : JpaRepository<FeePayment, Long> {
    fun findByFeeId(feeId: Long): List<FeePayment>
    fun findByFeeIdAndUserId(feeId: Long, userId: Long): FeePayment?
    fun countByFeeIdAndStatus(feeId: Long, status: PaymentStatus): Long

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(p.amountPaid), 0) FROM FeePayment p " +
            "WHERE p.fee.team.id = :teamId AND p.status <> com.dugout.api.domain.fee.entity.PaymentStatus.UNPAID",
    )
    fun sumCollectedByTeam(teamId: Long): Long

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(p.fee.amount - p.amountPaid), 0) FROM FeePayment p " +
            "WHERE p.fee.team.id = :teamId AND p.status <> com.dugout.api.domain.fee.entity.PaymentStatus.PAID",
    )
    fun sumOutstandingByTeam(teamId: Long): Long
}
