package com.dugout.api.domain.user.controller

import com.dugout.api.domain.user.dto.UserResponse
import com.dugout.api.domain.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class UserController(private val userService: UserService) {

    @GetMapping("/users/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getMe(userId))
}
