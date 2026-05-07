package com.dugout.api.domain.matching.entity

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TeamRatingTest {

    private fun teamWithDivision(division: Int): Team =
        Team.create(name = "팀$division", region = "서울", division = division)

    @Test
    fun `초기 ELO - 부수별로 1600·1400·1200·1000`() {
        assertEquals(1600, TeamRating.initialEloByDivision(1))
        assertEquals(1400, TeamRating.initialEloByDivision(2))
        assertEquals(1200, TeamRating.initialEloByDivision(3))
        assertEquals(1000, TeamRating.initialEloByDivision(4))
    }

    @Test
    fun `applyResult - 승리하면 ELO 증가, wins 증가`() {
        val rating = TeamRating(team = teamWithDivision(3), eloRating = 1200)
        val before = rating.eloRating

        rating.applyResult(opponentElo = 1200, result = 1.0)

        assertTrue(rating.eloRating > before)
        assertEquals(1, rating.wins)
        assertEquals(0, rating.losses)
    }

    @Test
    fun `applyResult - 동등 ELO 끼리 비기면 변화 없음`() {
        val rating = TeamRating(team = teamWithDivision(3), eloRating = 1200)

        rating.applyResult(opponentElo = 1200, result = 0.5)

        assertEquals(1200, rating.eloRating)
        assertEquals(1, rating.draws)
    }

    @Test
    fun `applyResult - 약팀이 강팀에게 이기면 큰 폭 상승`() {
        val rating = TeamRating(team = teamWithDivision(4), eloRating = 1000)

        rating.applyResult(opponentElo = 1600, result = 1.0)

        // K=32, expected_p ≈ 0.031, gain ≈ 31
        assertTrue(rating.eloRating - 1000 in 25..32) {
            "예상 25~32점 상승, 실제: ${rating.eloRating - 1000}"
        }
    }

    @Test
    fun `applyResult - 강팀이 약팀에게 지면 큰 폭 하락`() {
        val rating = TeamRating(team = teamWithDivision(1), eloRating = 1600)

        rating.applyResult(opponentElo = 1000, result = 0.0)

        // expected_p ≈ 0.969, loss ≈ 31
        assertTrue(1600 - rating.eloRating in 25..32) {
            "예상 25~32점 하락, 실제: ${1600 - rating.eloRating}"
        }
    }

    @Test
    fun `applyResult - 잘못된 result값이면 BusinessException(INVALID_MATCH_RESULT)`() {
        val rating = TeamRating(team = teamWithDivision(3), eloRating = 1200)

        val exception = assertThrows<BusinessException> {
            rating.applyResult(opponentElo = 1200, result = 0.7)
        }
        assertEquals(ErrorCode.INVALID_MATCH_RESULT, exception.errorCode)
    }
}
