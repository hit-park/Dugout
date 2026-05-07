package com.dugout.api.domain.ground.dto

import com.dugout.api.domain.ground.entity.FieldType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateGroundRequest(
    @field:NotBlank(message = "구장명은 필수입니다")
    @field:Size(max = 100, message = "구장명은 최대 100자입니다")
    val name: String,

    @field:NotBlank(message = "주소는 필수입니다")
    @field:Size(max = 200, message = "주소는 최대 200자입니다")
    val address: String,

    @field:NotNull(message = "위도는 필수입니다")
    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val latitude: Double,

    @field:NotNull(message = "경도는 필수입니다")
    @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    val longitude: Double,

    @field:NotNull(message = "구장 종류는 필수입니다")
    val fieldType: FieldType,

    val phone: String? = null,
    val hasLights: Boolean = false,
    val hasScoreboard: Boolean = false,
    val hasDugout: Boolean = false,
    val capacity: Int? = null,
    val priceInfo: String? = null,
    val photos: List<String> = emptyList(),
)

data class CreateGroundReviewRequest(
    @field:NotNull(message = "평점은 필수입니다")
    @field:Min(value = 1, message = "평점은 1 이상이어야 합니다")
    @field:Max(value = 5, message = "평점은 5 이하여야 합니다")
    val rating: Int,

    val content: String? = null,
    val photos: List<String> = emptyList(),
)
