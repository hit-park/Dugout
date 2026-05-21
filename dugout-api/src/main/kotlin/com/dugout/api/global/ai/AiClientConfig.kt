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

    /**
     * Spring Boot 가 자동 설정한 `RestClient.Builder` 빈을 주입받아 사용한다.
     * 빌더에 글로벌 ObjectMapper(application.yml 의 `spring.jackson.property-naming-strategy=SNAKE_CASE`
     * 적용된 것) 가 등록된 MessageConverter 들이 들어있어 dugout-ai 의 Pydantic snake_case 스키마와 매칭된다.
     *
     * 정적 `RestClient.builder()` 를 쓰면 default 컨버터(camelCase) 가 사용되어 dugout-ai 가
     * 422 Unprocessable Entity 로 응답하는 회귀가 발생하므로 사용 금지.
     */
    @Bean
    fun dugoutAiRestClient(builder: RestClient.Builder): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(aiProperties.timeoutMs))
            setReadTimeout(Duration.ofMillis(aiProperties.timeoutMs))
        }
        return builder
            .baseUrl(aiProperties.baseUrl)
            .requestFactory(factory)
            .build()
    }
}
