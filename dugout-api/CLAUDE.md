# CLAUDE.md — dugout-api (백엔드)

> 루트 [`/CLAUDE.md`](../CLAUDE.md) 의 모든 규칙이 적용된다. 이 파일은 백엔드 모듈 한정 추가 규칙.

## 빠른 시작

```bash
./gradlew build
./gradlew test
./gradlew bootRun                                   # 로컬 실행 (포트 8080)
./gradlew compileKotlin compileTestKotlin --quiet   # 빠른 컴파일 점검
./gradlew ktlintCheck                               # 린트
```

## 기술 스택 (실제 적용 상태)

- **Java 21 + Kotlin 2.1.0** (toolchain JVM 21)
- **Spring Boot 3.4.1**: web, data-jpa, data-redis, security, webflux, validation
- **Kotlin 코루틴**: kotlinx-coroutines-core, -reactor
- **JPA + Hibernate** (PostgreSQL 런타임, H2 in-memory 테스트)
- **QueryDSL: 미적용** (필요 시 도입 예정 — 루트 CLAUDE.md의 언급은 향후 계획)
- **JWT**: jjwt 0.12.6 + nimbus-jose-jwt 9.41.2 (Apple JWKS 검증용)
- **테스트**: JUnit5 + spring-boot-starter-test + mockito-kotlin 5.4.0

## 패키지 구조 (`com.dugout.api`)

```
com.dugout.api/
├── DugoutApplication.kt      # @SpringBootApplication
├── HealthController.kt       # GET /api/v1/health
├── domain/                   # 도메인별 5분할 (controller/service/repository/entity/dto)
│   ├── user/
│   ├── team/
│   ├── match/
│   └── attendance/
└── global/
    ├── auth/                 # OAuth 4종 (Kakao/Google/Apple/Naver) + JWT
    ├── config/               # SecurityConfig, JpaConfig, WebConfig
    ├── common/               # BaseEntity, PageResponse, StringListConverter
    └── error/                # ErrorCode, BusinessException, GlobalExceptionHandler
```

## 새 도메인 추가 절차 (체크리스트)

1. `domain/{새도메인}/` 아래 5폴더 생성: `controller`, `service`, `repository`, `entity`, `dto`
2. Entity는 `BaseEntity` 상속 (createdAt, updatedAt, deletedAt 자동 관리)
3. Repository는 `JpaRepository<T, Long>` 상속
4. Controller URL: `/api/v1/{도메인복수형}` (예: `/api/v1/teams`)
5. DTO는 `*Request`, `*Response` 분리 (`data class`)
6. **`docs/TDD.md` 갱신**: DB 스키마 + API 설계 섹션
7. ErrorCode 추가 시 `global/error/ErrorCode.kt` 에 enum entry 추가

## 에러 핸들링 (필수 패턴)

모든 비즈니스 에러는 `BusinessException` + `ErrorCode` enum 기반:

```kotlin
// global/error/ErrorCode.kt
enum class ErrorCode(val status: HttpStatus, val message: String) {
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다"),
    // ...
}

// 도메인 service에서 사용
throw BusinessException(ErrorCode.TEAM_NOT_FOUND)
```

`GlobalExceptionHandler`가 `BusinessException`을 표준 에러 응답으로 변환.

**금지**:
- raw `RuntimeException`, `IllegalArgumentException`, `IllegalStateException` 사용
- `throw Exception("...")` 형태의 임의 메시지
- Controller에서 try/catch로 직접 응답 가공 (Handler에 위임)

## OAuth 인증

`global/auth/`에 4종 OAuth 클라이언트:
- `KakaoOAuthClient`, `GoogleOAuthClient`, `AppleOAuthClient`, `NaverOAuthClient`
- 모두 `OAuthClient` 인터페이스 구현, `OAuthClientFactory`로 분기
- Apple은 JWKS 기반 서명 검증 (nimbus-jose-jwt)
- 토큰 발급: `JwtProvider`, 검증: `JwtFilter` (Spring Security FilterChain)

키·secret은 `application.yml` 직접 박기 금지 → 환경변수 참조.

## API 설계 규칙

- 모든 endpoint는 `/api/v1/*` prefix
- 페이징은 `PageResponse<T>` 사용 (`global/common`)
- 요청 검증은 `@Valid` + Bean Validation 어노테이션
- 응답은 항상 `*Response` DTO (Entity 직접 노출 금지)
- HTTP 상태: 생성 201, 조회 200, 삭제 204, 비즈니스 에러는 ErrorCode가 정의

## 코루틴 사용

- 외부 API 호출(OAuth 등)은 `suspend fun` + WebFlux WebClient
- DB 트랜잭션은 표준 JPA (코루틴 컨텍스트 전파 주의)
- 컨트롤러는 `suspend fun` 또는 일반 함수 모두 가능 (Spring Boot 3.x 지원)

## 테스트 가이드

- 단위 테스트: `*ServiceTest.kt` (Mockito-Kotlin)
- 통합 테스트: `@SpringBootTest` + H2 in-memory
- 픽스처에 실제 형식의 개인정보(전화번호 010-XXXX-XXXX 등) 절대 금지 — `010-0000-0001` 같은 합성 데이터만
- `application-test.yml` 분리하여 운영 키와 격리

## 보안 체크리스트 (PR 전 자체 점검)

- [ ] `application.yml`에 키·시크릿 직접 박지 않았는가? (모두 환경변수)
- [ ] 새 endpoint에 `SecurityConfig`의 인증 규칙 정합한가?
- [ ] 응답 DTO에 PII가 평문으로 포함되지 않는가? (마스킹 필요)
- [ ] 에러 응답에 stack trace / 내부 path 노출되지 않는가?
- [ ] `BusinessException` + `ErrorCode` 기반인가?
