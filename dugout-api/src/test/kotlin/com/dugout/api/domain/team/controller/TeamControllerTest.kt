package com.dugout.api.domain.team.controller

import com.dugout.api.domain.team.dto.InviteCodeResponse
import com.dugout.api.domain.team.dto.TeamResponse
import com.dugout.api.domain.team.service.TeamService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(TeamController::class)
@Import(SecurityConfig::class, JwtFilter::class)
class TeamControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var teamService: TeamService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val validToken = "valid-token"

    private fun setAuthenticated() {
        whenever(jwtProvider.validateToken(validToken)).thenReturn(true)
        whenever(jwtProvider.getUserId(validToken)).thenReturn(1L)
    }

    private fun sampleTeamResponse() = TeamResponse(
        id = 1L,
        name = "두갓FC",
        logoUrl = null,
        region = "서울 강남구",
        division = 4,
        homeGroundId = null,
        activityDays = listOf("SAT"),
        activityTime = "10:00",
        inviteCode = null,
        lineupMode = "BALANCED",
        memberCount = 1,
        createdAt = LocalDateTime.now(),
    )

    @Test
    fun `POST teams - 팀 생성 성공 201`() {
        setAuthenticated()
        whenever(teamService.createTeam(eq(1L), any())).thenReturn(sampleTeamResponse())

        mockMvc.perform(
            post("/api/v1/teams")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "두갓FC", "region": "서울 강남구"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("두갓FC"))
            .andExpect(jsonPath("$.member_count").value(1))
    }

    @Test
    fun `POST teams - 인증 없이 접근 시 4xx`() {
        mockMvc.perform(
            post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "두갓FC", "region": "서울"}"""),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `POST teams - name 누락 시 400`() {
        setAuthenticated()

        mockMvc.perform(
            post("/api/v1/teams")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "", "region": "서울"}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET teams - 팀 조회 성공`() {
        setAuthenticated()
        whenever(teamService.getTeam(1L)).thenReturn(sampleTeamResponse())

        mockMvc.perform(
            get("/api/v1/teams/1")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("두갓FC"))
    }

    @Test
    fun `POST teams invite - 초대 코드 생성 성공`() {
        setAuthenticated()
        whenever(teamService.generateInviteCode(1L, 1L)).thenReturn(InviteCodeResponse("ABCD1234"))

        mockMvc.perform(
            post("/api/v1/teams/1/invite")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.invite_code").value("ABCD1234"))
    }

    @Test
    fun `POST teams join - invite_code 누락 시 400`() {
        setAuthenticated()

        mockMvc.perform(
            post("/api/v1/teams/join")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"invite_code": ""}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET users me teams - 내 팀 목록 조회 성공`() {
        setAuthenticated()
        whenever(teamService.getMyTeams(1L)).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/v1/users/me/teams")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `DELETE teams members - 멤버 제거 성공 204`() {
        setAuthenticated()

        mockMvc.perform(
            delete("/api/v1/teams/1/members/10")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isNoContent)
    }
}
