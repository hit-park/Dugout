package com.dugout.api.domain.attendance.controller

import com.dugout.api.domain.attendance.dto.AttendanceResponse
import com.dugout.api.domain.attendance.dto.AttendanceSummaryResponse
import com.dugout.api.domain.attendance.service.AttendanceService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AttendanceController::class)
@Import(SecurityConfig::class, JwtFilter::class)
class AttendanceControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var attendanceService: AttendanceService
    @MockitoBean lateinit var jwtProvider: JwtProvider

    private val validToken = "valid-token"

    private fun setAuthenticated() {
        whenever(jwtProvider.validateToken(validToken)).thenReturn(true)
        whenever(jwtProvider.getUserId(validToken)).thenReturn(1L)
    }

    private fun sampleResponse() = AttendanceResponse(
        id = 1L,
        matchId = 1L,
        userId = 1L,
        nickname = "김주장",
        status = "ATTEND",
        reason = null,
        respondedAt = LocalDateTime.now(),
    )

    @Test
    fun `POST attendance - 출석 투표 성공 201`() {
        setAuthenticated()
        whenever(attendanceService.vote(eq(1L), eq(1L), any())).thenReturn(sampleResponse())

        mockMvc.perform(
            post("/api/v1/matches/1/attendance")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "ATTEND"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("ATTEND"))
    }

    @Test
    fun `POST attendance - 조퇴 투표 성공`() {
        setAuthenticated()
        whenever(attendanceService.vote(eq(1L), eq(1L), any())).thenReturn(
            sampleResponse().copy(status = "EARLY_LEAVE", reason = "5회말 종료 후 퇴장"),
        )

        mockMvc.perform(
            post("/api/v1/matches/1/attendance")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "EARLY_LEAVE", "reason": "5회말 종료 후 퇴장"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("EARLY_LEAVE"))
    }

    @Test
    fun `POST attendance - status 누락 시 400`() {
        setAuthenticated()

        mockMvc.perform(
            post("/api/v1/matches/1/attendance")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": ""}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT attendance - 출석 변경 성공`() {
        setAuthenticated()
        whenever(attendanceService.updateVote(eq(1L), eq(1L), any())).thenReturn(
            sampleResponse().copy(status = "LATE"),
        )

        mockMvc.perform(
            put("/api/v1/matches/1/attendance")
                .header("Authorization", "Bearer $validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "LATE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("LATE"))
    }

    @Test
    fun `GET attendance - 출석 현황 조회 성공`() {
        setAuthenticated()
        whenever(attendanceService.getAttendanceSummary(1L)).thenReturn(
            AttendanceSummaryResponse(
                matchId = 1L,
                totalMembers = 15,
                respondedCount = 10,
                pendingCount = 5,
                statusCounts = mapOf("ATTEND" to 8, "ABSENT" to 1, "LATE" to 1),
                votes = emptyList(),
            ),
        )

        mockMvc.perform(
            get("/api/v1/matches/1/attendance")
                .header("Authorization", "Bearer $validToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total_members").value(15))
            .andExpect(jsonPath("$.pending_count").value(5))
    }
}
