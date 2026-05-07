package com.dugout.api.domain.mercenary.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class ProfileUpsertRequest(
    val isActive: Boolean? = null,
    val regions: List<String>? = null,
    val availableDays: List<String>? = null,
    val availableTimes: List<String>? = null,
    val positions: List<String>? = null,
    val desiredFee: Long? = null,
)

data class CreateMercenaryRequestPayload(
    @field:NotNull(message = "경기 ID는 필수입니다")
    val matchId: Long,

    @field:NotNull(message = "필요 포지션은 필수입니다")
    val neededPositions: List<String>,

    @field:NotNull(message = "필요 인원은 필수입니다")
    @field:Min(value = 1, message = "필요 인원은 1명 이상이어야 합니다")
    val neededCount: Int,

    val skillMin: Int? = null,
    val skillMax: Int? = null,

    @field:PositiveOrZero(message = "용병료는 0 이상이어야 합니다")
    val fee: Long? = null,

    val regions: List<String> = emptyList(),
    val memo: String? = null,
)

data class ApplyMercenaryRequest(
    @field:NotBlank(message = "지원 포지션은 필수입니다")
    val position: String,

    val memo: String? = null,
)
