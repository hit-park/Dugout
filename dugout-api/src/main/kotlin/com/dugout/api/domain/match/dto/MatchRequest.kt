package com.dugout.api.domain.match.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class CreateMatchRequest(
    @field:NotNull(message = "경기 날짜는 필수입니다")
    val matchDate: LocalDate,

    @field:NotNull(message = "경기 시간은 필수입니다")
    val matchTime: LocalTime,

    val gatherTime: LocalTime? = null,
    val opponentName: String? = null,
    val opponentTeamId: Long? = null,
    val groundId: Long? = null,
    val groundName: String? = null,
    val voteDeadline: LocalDateTime? = null,
    val memo: String? = null,
)

data class UpdateMatchRequest(
    val matchDate: LocalDate? = null,
    val matchTime: LocalTime? = null,
    val gatherTime: LocalTime? = null,
    val opponentName: String? = null,
    val opponentTeamId: Long? = null,
    val groundId: Long? = null,
    val groundName: String? = null,
    val voteDeadline: LocalDateTime? = null,
    val memo: String? = null,
)
