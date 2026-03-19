package com.dugout.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {

    data class HealthResponse(
        val status: String = "UP",
        val timestamp: Instant = Instant.now(),
    )

    @GetMapping("/api/v1/health")
    fun health(): HealthResponse = HealthResponse()
}
