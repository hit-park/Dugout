package com.dugout.api.global.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(AiProperties::class)
class AiClientConfig(
    private val aiProperties: AiProperties,
) {

    @Bean
    fun dugoutAiRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(aiProperties.timeoutMs))
            setReadTimeout(Duration.ofMillis(aiProperties.timeoutMs))
        }
        return RestClient.builder()
            .baseUrl(aiProperties.baseUrl)
            .requestFactory(factory)
            .build()
    }
}
