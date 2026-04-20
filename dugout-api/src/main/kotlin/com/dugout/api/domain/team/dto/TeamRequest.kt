package com.dugout.api.domain.team.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateTeamRequest(
    @field:NotBlank(message = "팀 이름은 필수입니다")
    val name: String,

    @field:NotBlank(message = "지역은 필수입니다")
    val region: String,

    @field:Min(value = 1, message = "부수는 1 이상입니다")
    val division: Int? = null,

    val logoUrl: String? = null,
    val activityDays: List<String>? = null,
    val activityTime: String? = null,
    val lineupMode: String? = null,
)

data class UpdateTeamRequest(
    val name: String? = null,
    val logoUrl: String? = null,
    val region: String? = null,
    val division: Int? = null,
    val activityDays: List<String>? = null,
    val activityTime: String? = null,
    val lineupMode: String? = null,
)

data class JoinTeamRequest(
    @field:NotBlank(message = "초대 코드는 필수입니다")
    val inviteCode: String,
)

data class UpdateMemberRequest(
    val role: String? = null,
    val jerseyNumber: Int? = null,
    val positions: List<String>? = null,
)
