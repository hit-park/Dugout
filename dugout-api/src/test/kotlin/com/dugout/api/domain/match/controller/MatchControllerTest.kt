package com.dugout.api.domain.match.controller

import com.dugout.api.domain.match.dto.MatchResponse
import com.dugout.api.domain.match.service.MatchService
import com.dugout.api.global.auth.JwtFilter
import com.dugout.api.global.auth.JwtProvider
import com.dugout.api.global.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@WebMvcTest(MatchController::class)
@Import(SecurityConfig::class, JwtFilter::class)
class MatchControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var matchService: MatchService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val validToken = "valid-token"

    private fun setAuthenticated() {
        whenever(jwtProvider.validateToken(validToken)).thenReturn(true)
        whenever(jwtProvider.getUserId(validToken)).thenReturn(1L)
    }

    private fun sampleMatchResponse() = MatchResponse(
        id = 1L,
        teamId = 1L,
        opponentName = "파이어볼즈",
        opponentTeamId = null,
        groundId = null,
        groundName = "잠실야구장",
        matchDate = LocalDate.of(2026, 5, 1),
        gatherTime = LocalTime.of(9, 0),
        matchTime = LocalTime.of(10, 0),
        voteDeadline = null,
        status = "SCHEDULED",
        resultHome = null,
        resultAway = null,
        memo = null,
        createdAt = LocalDateTime.now(),
    )

    @Test
    fun `POST teams matches - 경기 생성 성공 201`() {
        setAuthenticated()
        whenever(matchService.createMatch(eq(1L), eq(1L), any())).thenReturn(sampleMatchResponse())

        mockMvc.perform(
            post("/api/v1/teams/1/matches")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "match_date": "2026-05-01",
                      "match_time": "10:00:00",
                      "opponent_name": "파이어볼즈"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.opponent_name").value("파이어볼즈"))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
    }

    @Test
    fun `POST teams matches - 인증 없으면 4xx`() {
        mockMvc.perform(
            post("/api/v1/teams/1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"match_date": "2026-05-01", "match_time": "10:00:00"}"""),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `GET matches - 경기 상세 조회 성공`() {
        setAuthenticated()
        whenever(matchService.getMatch(1L)).thenReturn(sampleMatchResponse())

        mockMvc.perform(
            get("/api/v1/matches/1")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `GET teams matches - 경기 목록 조회 성공`() {
        setAuthenticated()
        whenever(matchService.getMatches(eq(1L), any(), any())).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/v1/teams/1/matches")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isOk)
    }
}
