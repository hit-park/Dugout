package com.dugout.api.domain.mercenary.controller

import com.dugout.api.domain.mercenary.dto.ApplyMercenaryRequest
import com.dugout.api.domain.mercenary.dto.CreateMercenaryRequestPayload
import com.dugout.api.domain.mercenary.dto.MercenaryApplicationResponse
import com.dugout.api.domain.mercenary.dto.MercenaryProfileResponse
import com.dugout.api.domain.mercenary.dto.MercenaryRequestResponse
import com.dugout.api.domain.mercenary.dto.ProfileUpsertRequest
import com.dugout.api.domain.mercenary.service.MercenaryService
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
@RequestMapping("/api/v1")
class MercenaryController(
    private val mercenaryService: MercenaryService,
) {

    @PutMapping("/mercenary/profile")
    fun upsertProfile(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ProfileUpsertRequest,
    ): ResponseEntity<MercenaryProfileResponse> =
        ResponseEntity.ok(mercenaryService.upsertProfile(userId, request))

    @GetMapping("/mercenary/profile")
    fun getMyProfile(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<MercenaryProfileResponse> =
        ResponseEntity.ok(mercenaryService.getMyProfile(userId))

    @PostMapping("/teams/{teamId}/mercenary/requests")
    fun createRequest(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @Valid @RequestBody payload: CreateMercenaryRequestPayload,
    ): ResponseEntity<MercenaryRequestResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(mercenaryService.createRequest(userId, teamId, payload))

    @GetMapping("/mercenary/requests")
    fun listOpenRequests(): ResponseEntity<List<MercenaryRequestResponse>> =
        ResponseEntity.ok(mercenaryService.listOpenRequests())

    @GetMapping("/mercenary/requests/{requestId}")
    fun getRequest(@PathVariable requestId: Long): ResponseEntity<MercenaryRequestResponse> =
        ResponseEntity.ok(mercenaryService.getRequest(requestId))

    @PostMapping("/mercenary/requests/{requestId}/close")
    fun closeRequest(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
    ): ResponseEntity<MercenaryRequestResponse> =
        ResponseEntity.ok(mercenaryService.closeRequest(userId, requestId))

    @PostMapping("/mercenary/requests/{requestId}/apply")
    fun apply(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
        @Valid @RequestBody body: ApplyMercenaryRequest,
    ): ResponseEntity<MercenaryApplicationResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(mercenaryService.apply(userId, requestId, body))

    @GetMapping("/mercenary/requests/{requestId}/applications")
    fun listApplications(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
    ): ResponseEntity<List<MercenaryApplicationResponse>> =
        ResponseEntity.ok(mercenaryService.listApplications(userId, requestId))

    @PostMapping("/mercenary/requests/{requestId}/accept/{targetUserId}")
    fun acceptApplication(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
        @PathVariable targetUserId: Long,
    ): ResponseEntity<MercenaryApplicationResponse> =
        ResponseEntity.ok(mercenaryService.acceptApplication(userId, requestId, targetUserId))

    @PostMapping("/mercenary/requests/{requestId}/reject/{targetUserId}")
    fun rejectApplication(
        @AuthenticationPrincipal userId: Long,
        @PathVariable requestId: Long,
        @PathVariable targetUserId: Long,
    ): ResponseEntity<MercenaryApplicationResponse> =
        ResponseEntity.ok(mercenaryService.rejectApplication(userId, requestId, targetUserId))

    @GetMapping("/mercenary/requests/{requestId}/recommend")
    fun recommendCandidates(
        @PathVariable requestId: Long,
    ): ResponseEntity<List<MercenaryProfileResponse>> =
        ResponseEntity.ok(mercenaryService.recommendCandidates(requestId))
}
