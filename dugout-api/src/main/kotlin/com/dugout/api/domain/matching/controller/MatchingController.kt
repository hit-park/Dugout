package com.dugout.api.domain.matching.controller

import com.dugout.api.domain.matching.dto.CreateMatchingRequestPayload
import com.dugout.api.domain.matching.dto.MatchResultPayload
import com.dugout.api.domain.matching.dto.MatchingProposalResponse
import com.dugout.api.domain.matching.dto.MatchingRequestResponse
import com.dugout.api.domain.matching.dto.ProposeMatchPayload
import com.dugout.api.domain.matching.dto.TeamRatingResponse
import com.dugout.api.domain.matching.service.MatchingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/matching")
class MatchingController(
    private val matchingService: MatchingService,
) {

    @PostMapping("/teams/{teamId}/requests")
    fun createRequest(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @Valid @RequestBody payload: CreateMatchingRequestPayload,
    ): ResponseEntity<MatchingRequestResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(matchingService.createRequest(userId, teamId, payload))

    @GetMapping("/requests")
    fun listOpenRequests(): ResponseEntity<List<MatchingRequestResponse>> =
        ResponseEntity.ok(matchingService.listOpenRequests())

    @GetMapping("/requests/{requestId}")
    fun getRequest(@PathVariable requestId: Long): ResponseEntity<MatchingRequestResponse> =
        ResponseEntity.ok(matchingService.getRequest(requestId))

    @PostMapping("/requests/{requestId}/cancel")
    fun cancelRequest(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
    ): ResponseEntity<MatchingRequestResponse> =
        ResponseEntity.ok(matchingService.cancelRequest(userId, requestId))

    @PostMapping("/requests/{requestId}/propose/{proposingTeamId}")
    fun propose(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
        @PathVariable proposingTeamId: Long,
        @Valid @RequestBody payload: ProposeMatchPayload,
    ): ResponseEntity<MatchingProposalResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(matchingService.proposeMatch(userId, requestId, proposingTeamId, payload))

    @GetMapping("/requests/{requestId}/proposals")
    fun listProposals(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
    ): ResponseEntity<List<MatchingProposalResponse>> =
        ResponseEntity.ok(matchingService.listProposals(userId, requestId))

    @PostMapping("/proposals/{proposalId}/accept")
    fun accept(
        @AuthenticationPrincipal userId: Long,
        @PathVariable proposalId: Long,
    ): ResponseEntity<MatchingProposalResponse> =
        ResponseEntity.ok(matchingService.acceptProposal(userId, proposalId))

    @PostMapping("/proposals/{proposalId}/reject")
    fun reject(
        @AuthenticationPrincipal userId: Long,
        @PathVariable proposalId: Long,
    ): ResponseEntity<MatchingProposalResponse> =
        ResponseEntity.ok(matchingService.rejectProposal(userId, proposalId))

    @PostMapping("/results/{requestId}")
    fun applyResult(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
        @Valid @RequestBody payload: MatchResultPayload,
    ): ResponseEntity<MatchingRequestResponse> =
        ResponseEntity.ok(matchingService.applyResult(userId, requestId, payload))

    @GetMapping("/requests/{requestId}/recommend")
    fun recommend(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
    ): ResponseEntity<List<TeamRatingResponse>> =
        ResponseEntity.ok(matchingService.recommendOpponents(userId, requestId))

    @GetMapping("/teams/{teamId}/rating")
    fun getRating(@PathVariable teamId: Long): ResponseEntity<TeamRatingResponse> =
        ResponseEntity.ok(matchingService.getRating(teamId))
}
