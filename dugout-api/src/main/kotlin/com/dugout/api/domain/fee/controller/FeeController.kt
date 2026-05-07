package com.dugout.api.domain.fee.controller

import com.dugout.api.domain.fee.dto.CreateFeeRequest
import com.dugout.api.domain.fee.dto.FeePaymentResponse
import com.dugout.api.domain.fee.dto.FeeResponse
import com.dugout.api.domain.fee.dto.FinanceSummaryResponse
import com.dugout.api.domain.fee.dto.ProcessPaymentRequest
import com.dugout.api.domain.fee.dto.UpdateFeeRequest
import com.dugout.api.domain.fee.service.FeeService
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
class FeeController(
    private val feeService: FeeService,
) {

    @PostMapping("/teams/{teamId}/fees")
    fun createFee(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
        @Valid @RequestBody request: CreateFeeRequest,
    ): ResponseEntity<FeeResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(feeService.createFee(userId, teamId, request))

    @GetMapping("/teams/{teamId}/fees")
    fun getFees(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
    ): ResponseEntity<List<FeeResponse>> =
        ResponseEntity.ok(feeService.getFees(userId, teamId))

    @PutMapping("/fees/{feeId}")
    fun updateFee(
        @AuthenticationPrincipal userId: Long,
        @PathVariable feeId: Long,
        @Valid @RequestBody request: UpdateFeeRequest,
    ): ResponseEntity<FeeResponse> =
        ResponseEntity.ok(feeService.updateFee(userId, feeId, request))

    @GetMapping("/fees/{feeId}/payments")
    fun getPayments(
        @AuthenticationPrincipal userId: Long,
        @PathVariable feeId: Long,
    ): ResponseEntity<List<FeePaymentResponse>> =
        ResponseEntity.ok(feeService.getPayments(userId, feeId))

    @PostMapping("/fees/{feeId}/payments/{targetUserId}")
    fun processPayment(
        @AuthenticationPrincipal userId: Long,
        @PathVariable feeId: Long,
        @PathVariable targetUserId: Long,
        @Valid @RequestBody request: ProcessPaymentRequest,
    ): ResponseEntity<FeePaymentResponse> =
        ResponseEntity.ok(feeService.processPayment(userId, feeId, targetUserId, request))

    @GetMapping("/teams/{teamId}/finance/summary")
    fun getFinanceSummary(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
    ): ResponseEntity<FinanceSummaryResponse> =
        ResponseEntity.ok(feeService.getFinanceSummary(userId, teamId))
}
