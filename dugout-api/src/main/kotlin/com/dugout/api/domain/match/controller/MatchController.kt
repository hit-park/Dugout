package com.dugout.api.domain.match.controller

import com.dugout.api.domain.match.dto.CreateMatchRequest
import com.dugout.api.domain.match.dto.MatchResponse
import com.dugout.api.domain.match.dto.UpdateMatchRequest
import com.dugout.api.domain.match.service.MatchService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
class MatchController(
    private val matchService: MatchService,
) {

    @PostMapping("/teams/{teamId}/matches")
    fun createMatch(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @Valid @RequestBody request: CreateMatchRequest,
    ): ResponseEntity<MatchResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(matchService.createMatch(userId, teamId, request))

    @GetMapping("/teams/{teamId}/matches")
    fun getMatches(
        @PathVariable teamId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<List<MatchResponse>> =
        ResponseEntity.ok(matchService.getMatches(teamId, from, to))

    @GetMapping("/matches/{matchId}")
    fun getMatch(@PathVariable matchId: Long): ResponseEntity<MatchResponse> =
        ResponseEntity.ok(matchService.getMatch(matchId))

    @PutMapping("/matches/{matchId}")
    fun updateMatch(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
        @Valid @RequestBody request: UpdateMatchRequest,
    ): ResponseEntity<MatchResponse> =
        ResponseEntity.ok(matchService.updateMatch(userId, matchId, request))

    @DeleteMapping("/matches/{matchId}")
    fun cancelMatch(
        @AuthenticationPrincipal userId: Long,
        @PathVariable matchId: Long,
    ): ResponseEntity<Void> {
        matchService.cancelMatch(userId, matchId)
        return ResponseEntity.noContent().build()
    }
}
