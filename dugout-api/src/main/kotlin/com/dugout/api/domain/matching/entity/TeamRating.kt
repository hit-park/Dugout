package com.dugout.api.domain.matching.entity

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kotlin.math.pow
import kotlin.math.roundToInt

@Entity
@Table(name = "team_ratings")
class TeamRating(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    val team: Team,

    @Column(name = "elo_rating", nullable = false)
    var eloRating: Int,

    @Column(nullable = false)
    var wins: Int = 0,

    @Column(nullable = false)
    var losses: Int = 0,

    @Column(nullable = false)
    var draws: Int = 0,

    @Column(name = "manner_score", nullable = false)
    var mannerScore: Double = 0.0,

    @Column(name = "manner_count", nullable = false)
    var mannerCount: Int = 0,
) : BaseEntity() {

    /**
     * 표준 ELO 갱신.
     *
     * @param opponentElo 상대 팀 ELO
     * @param result 1.0(승), 0.5(무), 0.0(패)
     */
    fun applyResult(opponentElo: Int, result: Double) {
        if (result !in setOf(0.0, 0.5, 1.0)) {
            throw BusinessException(ErrorCode.INVALID_MATCH_RESULT)
        }
        val expected = 1.0 / (1.0 + 10.0.pow((opponentElo - eloRating) / 400.0))
        eloRating = (eloRating + K * (result - expected)).roundToInt()
        when (result) {
            1.0 -> wins++
            0.0 -> losses++
            else -> draws++
        }
    }

    fun addMannerScore(newScore: Double) {
        val total = mannerScore * mannerCount + newScore
        mannerCount += 1
        mannerScore = total / mannerCount
    }

    companion object {
        const val K = 32

        fun initialEloByDivision(division: Int): Int = when (division) {
            1 -> 1600
            2 -> 1400
            3 -> 1200
            else -> 1000
        }

        fun ofTeam(team: Team): TeamRating = TeamRating(
            team = team,
            eloRating = initialEloByDivision(team.division),
        )
    }
}
