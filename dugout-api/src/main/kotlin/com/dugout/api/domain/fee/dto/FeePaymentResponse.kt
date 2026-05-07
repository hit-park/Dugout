package com.dugout.api.domain.fee.dto

import com.dugout.api.domain.fee.entity.FeePayment
import java.time.LocalDateTime

data class FeePaymentResponse(
    val id: Long,
    val feeId: Long,
    val userId: Long,
    val userNickname: String,
    val amountDue: Long,
    val amountPaid: Long,
    val status: String,
    val paidAt: LocalDateTime?,
    val confirmedBy: Long?,
    val memo: String?,
) {
    companion object {
        fun from(payment: FeePayment): FeePaymentResponse = FeePaymentResponse(
            id = payment.id,
            feeId = payment.fee.id,
            userId = payment.user.id,
            userNickname = payment.user.nickname,
            amountDue = payment.fee.amount,
            amountPaid = payment.amountPaid,
            status = payment.status.name,
            paidAt = payment.paidAt,
            confirmedBy = payment.confirmedBy,
            memo = payment.memo,
        )
    }
}
