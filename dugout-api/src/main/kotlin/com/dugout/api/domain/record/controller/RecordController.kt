package com.dugout.api.domain.record.controller

import com.dugout.api.domain.record.dto.BattingStatsResponse
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.dto.PlateAppearanceResponse
import com.dugout.api.domain.record.service.RecordService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val recordService: RecordService,
) {

    @PostMapping("/plate-appearances")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreatePlateAppearanceRequest,
    ): ResponseEntity<PlateAppearanceResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(recordService.create(userId, request))

    @GetMapping("/plate-appearances")
    fun listByMatch(
        @AuthenticationPrincipal userId: Long,
        @RequestParam matchId: Long,
    ): ResponseEntity<List<PlateAppearanceResponse>> =
        ResponseEntity.ok(recordService.listByMatch(userId, matchId))

    @DeleteMapping("/plate-appearances/{recordId}")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable recordId: Long,
    ): ResponseEntity<Void> {
        recordService.delete(userId, recordId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/teams/{teamId}/batting-stats")
    fun battingStats(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
    ): ResponseEntity<List<BattingStatsResponse>> =
        ResponseEntity.ok(recordService.battingStats(userId, teamId))
}
