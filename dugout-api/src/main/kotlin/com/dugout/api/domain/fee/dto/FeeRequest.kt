package com.dugout.api.domain.fee.dto

import com.dugout.api.domain.fee.entity.FeeType
import com.dugout.api.domain.fee.entity.PaymentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateFeeRequest(
    @field:NotBlank(message = "회비 제목은 필수입니다")
    @field:Size(max = 100, message = "회비 제목은 최대 100자입니다")
    val title: String,

    @field:NotNull(message = "회비 금액은 필수입니다")
    @field:Positive(message = "회비 금액은 0보다 커야 합니다")
    val amount: Long,

    @field:NotNull(message = "회비 유형은 필수입니다")
    val feeType: FeeType,

    @field:NotNull(message = "납부 기한은 필수입니다")
    val dueDate: LocalDate,

    val matchId: Long? = null,
    val memo: String? = null,
)

data class UpdateFeeRequest(
    @field:Size(max = 100, message = "회비 제목은 최대 100자입니다")
    val title: String? = null,

    @field:Positive(message = "회비 금액은 0보다 커야 합니다")
    val amount: Long? = null,

    val dueDate: LocalDate? = null,
    val memo: String? = null,
)

data class ProcessPaymentRequest(
    @field:NotNull(message = "납부 상태는 필수입니다")
    val status: PaymentStatus,

    @field:NotNull(message = "납부 금액은 필수입니다")
    @field:PositiveOrZero(message = "납부 금액은 0 이상이어야 합니다")
    val amountPaid: Long,

    val memo: String? = null,
)
