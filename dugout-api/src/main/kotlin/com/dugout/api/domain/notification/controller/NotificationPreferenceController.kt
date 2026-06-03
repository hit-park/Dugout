package com.dugout.api.domain.notification.controller

import com.dugout.api.domain.notification.dto.NotificationPreferenceRequest
import com.dugout.api.domain.notification.dto.NotificationPreferenceResponse
import com.dugout.api.domain.notification.service.NotificationPreferenceService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
class NotificationPreferenceController(
    private val service: NotificationPreferenceService,
) {
    @GetMapping
    fun get(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<NotificationPreferenceResponse> =
        ResponseEntity.ok(service.getOrCreate(userId))

    @PatchMapping
    fun update(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: NotificationPreferenceRequest,
    ): ResponseEntity<NotificationPreferenceResponse> =
        ResponseEntity.ok(service.update(userId, request))
}
