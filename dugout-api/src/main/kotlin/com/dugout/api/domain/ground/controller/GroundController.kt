package com.dugout.api.domain.ground.controller

import com.dugout.api.domain.ground.dto.CreateGroundRequest
import com.dugout.api.domain.ground.dto.CreateGroundReviewRequest
import com.dugout.api.domain.ground.dto.GroundResponse
import com.dugout.api.domain.ground.dto.GroundReviewResponse
import com.dugout.api.domain.ground.service.GroundService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/grounds")
class GroundController(
    private val groundService: GroundService,
) {

    @PostMapping
    fun createGround(
        @Valid @RequestBody request: CreateGroundRequest,
    ): ResponseEntity<GroundResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groundService.createGround(request))

    @GetMapping
    fun searchGrounds(
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(name = "radius_km", required = false) radiusKm: Double?,
    ): ResponseEntity<List<GroundResponse>> =
        ResponseEntity.ok(groundService.searchGrounds(lat, lng, radiusKm))

    @GetMapping("/{groundId}")
    fun getGround(@PathVariable groundId: Long): ResponseEntity<GroundResponse> =
        ResponseEntity.ok(groundService.getGround(groundId))

    @GetMapping("/{groundId}/reviews")
    fun getReviews(@PathVariable groundId: Long): ResponseEntity<List<GroundReviewResponse>> =
        ResponseEntity.ok(groundService.getReviews(groundId))

    @PostMapping("/{groundId}/reviews")
    fun createReview(
        @AuthenticationPrincipal userId: Long,
        @PathVariable groundId: Long,
        @Valid @RequestBody request: CreateGroundReviewRequest,
    ): ResponseEntity<GroundReviewResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groundService.createReview(userId, groundId, request))
}
