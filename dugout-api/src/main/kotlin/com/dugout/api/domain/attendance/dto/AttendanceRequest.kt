package com.dugout.api.domain.attendance.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AttendanceVoteRequest(
    @field:NotBlank(message = "출석 상태는 필수입니다")
    val status: String,

    @field:Size(max = 200, message = "사유는 200자 이내로 입력해주세요")
    val reason: String? = null,
)
