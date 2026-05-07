package com.dugout.api.domain.fee.dto

data class FinanceSummaryResponse(
    val teamId: Long,
    val totalCollected: Long,
    val totalOutstanding: Long,
    val feeCount: Int,
)
