package com.dugout.api.global.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "dugout.ai")
data class AiProperties(
    val baseUrl: String,
    val timeoutMs: Long = 5000,
)
