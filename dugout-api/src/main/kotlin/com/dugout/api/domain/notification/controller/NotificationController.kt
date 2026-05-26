package com.dugout.api.domain.notification.controller

import com.dugout.api.domain.notification.dto.FcmTokenRequest
import com.dugout.api.domain.notification.dto.FcmTokenResponse
import com.dugout.api.domain.notification.service.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @PatchMapping("/fcm-token")
    fun updateFcmToken(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: FcmTokenRequest,
    ): ResponseEntity<FcmTokenResponse> {
        notificationService.updateFcmToken(userId, request.token)
        return ResponseEntity.ok(FcmTokenResponse(ok = true))
    }
}
