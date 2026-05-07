package com.dugout.api.domain.lineup.controller

import com.dugout.api.domain.lineup.dto.LineupRecommendationResponse
import com.dugout.api.domain.lineup.dto.LineupResponse
import com.dugout.api.domain.lineup.dto.SaveLineupRequest
import com.dugout.api.domain.lineup.service.LineupService
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
@RequestMapping("/api/v1/matches/{matchId}/lineup")
class LineupController(
    private val lineupService: LineupService,
) {

    @PostMapping("/recommend")
    fun recommend(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
    ): ResponseEntity<LineupRecommendationResponse> =
        ResponseEntity.ok(lineupService.recommend(userId, matchId))

    @PostMapping
    fun saveLineup(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
        @Valid @RequestBody request: SaveLineupRequest,
    ): ResponseEntity<LineupResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(lineupService.saveLineup(userId, matchId, request))

    @PutMapping
    fun updateLineup(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
        @Valid @RequestBody request: SaveLineupRequest,
    ): ResponseEntity<LineupResponse> =
        ResponseEntity.ok(lineupService.updateLineup(userId, matchId, request))

    @GetMapping
    fun getLineup(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
    ): ResponseEntity<LineupResponse> =
        ResponseEntity.ok(lineupService.getLineup(userId, matchId))

    @PostMapping("/confirm")
    fun confirmLineup(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
    ): ResponseEntity<LineupResponse> =
        ResponseEntity.ok(lineupService.confirmLineup(userId, matchId))

    @GetMapping("/card")
    fun getLineupCard(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
    ): ResponseEntity<LineupResponse> =
        ResponseEntity.ok(lineupService.getLineupCard(userId, matchId))
}
