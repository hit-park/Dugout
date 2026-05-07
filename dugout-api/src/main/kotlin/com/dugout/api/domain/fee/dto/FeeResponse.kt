package com.dugout.api.domain.fee.dto

import com.dugout.api.domain.fee.entity.Fee
import java.time.LocalDate
import java.time.LocalDateTime

data class FeeResponse(
    val id: Long,
    val teamId: Long,
    val title: String,
    val amount: Long,
    val feeType: String,
    val matchId: Long?,
    val dueDate: LocalDate,
    val memo: String?,
    val totalCount: Long,
    val paidCount: Long,
    val unpaidCount: Long,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(
            fee: Fee,
            totalCount: Long,
            paidCount: Long,
            unpaidCount: Long,
        ): FeeResponse = FeeResponse(
            id = fee.id,
            teamId = fee.team.id,
            title = fee.title,
            amount = fee.amount,
            feeType = fee.feeType.name,
            matchId = fee.matchId,
            dueDate = fee.dueDate,
            memo = fee.memo,
            totalCount = totalCount,
            paidCount = paidCount,
            unpaidCount = unpaidCount,
            createdAt = fee.createdAt,
        )
    }
}
