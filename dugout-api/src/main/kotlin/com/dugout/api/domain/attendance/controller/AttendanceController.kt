package com.dugout.api.domain.attendance.controller

import com.dugout.api.domain.attendance.dto.AttendanceResponse
import com.dugout.api.domain.attendance.dto.AttendanceSummaryResponse
import com.dugout.api.domain.attendance.dto.AttendanceVoteRequest
import com.dugout.api.domain.attendance.service.AttendanceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/matches/{matchId}/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService,
) {

    @PostMapping
    fun vote(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
        @Valid @RequestBody request: AttendanceVoteRequest,
    ): ResponseEntity<AttendanceResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.vote(userId, matchId, request))

    @PutMapping
    fun updateVote(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
        @Valid @RequestBody request: AttendanceVoteRequest,
    ): ResponseEntity<AttendanceResponse> =
        ResponseEntity.ok(attendanceService.updateVote(userId, matchId, request))

    @GetMapping
    fun getSummary(@PathVariable matchId: Long): ResponseEntity<AttendanceSummaryResponse> =
        ResponseEntity.ok(attendanceService.getAttendanceSummary(matchId))
}
