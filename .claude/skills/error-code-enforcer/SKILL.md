---
name: error-code-enforcer
description: Use when writing or reviewing Kotlin code in dugout-api/ — enforces the BusinessException + ErrorCode enum error handling pattern. Blocks raw RuntimeException / IllegalArgumentException / IllegalStateException usage in service and controller layers, and ensures all error responses go through GlobalExceptionHandler.
---

# 에러 핸들링 가드 (dugout-api)

dugout-api의 **모든 비즈니스 에러는 `BusinessException` + `ErrorCode` enum 기반**이다.
이 패턴을 우회하면 사용자에게 일관되지 않은 응답이 가고, `GlobalExceptionHandler`의 표준 변환이 적용되지 않는다.

---

## 1. 표준 패턴

### 정의 (이미 `global/error/`에 존재)

```kotlin
// ErrorCode.kt
enum class ErrorCode(val status: HttpStatus, val message: String) {
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다"),
    TEAM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 팀입니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    // ...
}

// BusinessException.kt
class BusinessException(
    val errorCode: ErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)
```

### 사용

```kotlin
@Service
@Transactional(readOnly = true)
class TeamService(
    private val teamRepository: TeamRepository,
) {
    fun get(id: Long): TeamResponse {
        val team = teamRepository.findById(id).orElseThrow {
            BusinessException(ErrorCode.TEAM_NOT_FOUND)
        }
        return TeamResponse.from(team)
    }
}
```

`GlobalExceptionHandler`가 `BusinessException`을 받아 `{ "code": "TEAM_NOT_FOUND", "message": "...", "status": 404 }` 형태로 변환.

---

## 2. 절대 금지 → 권장

| ❌ 금지 | ✅ 권장 |
|--------|---------|
| `throw RuntimeException("팀 없음")` | `throw BusinessException(ErrorCode.TEAM_NOT_FOUND)` |
| `throw IllegalArgumentException("...")` | `throw BusinessException(ErrorCode.INVALID_INPUT)` |
| `throw IllegalStateException("...")` | `throw BusinessException(ErrorCode.{ALREADY_EXISTS|...})` |
| `throw Exception("...")` | `throw BusinessException(...)` |
| `throw NullPointerException("...")` | 검증 가드 + `throw BusinessException(...)` |
| Controller try/catch로 응답 가공 | Service에서 throw → `GlobalExceptionHandler` 위임 |
| `ResponseEntity.badRequest().body(mapOf("error" to ...))` | `BusinessException` throw + 표준 응답 |

### Controller try/catch 안티패턴

```kotlin
// ❌ Controller가 직접 에러 응답 가공
@PostMapping
fun create(@RequestBody req: TeamRequest): ResponseEntity<*> {
    return try {
        val team = service.create(req)
        ResponseEntity.ok(team)
    } catch (e: Exception) {
        ResponseEntity.badRequest().body(mapOf("error" to e.message))
    }
}

// ✅ throw → GlobalExceptionHandler가 처리
@PostMapping
fun create(@Valid @RequestBody req: TeamRequest): ResponseEntity<TeamResponse> {
    val team = service.create(req)
    return ResponseEntity.status(HttpStatus.CREATED).body(team)
}
```

---

## 3. 새 ErrorCode 추가 규칙

| 항목 | 규칙 | 예 |
|------|------|-----|
| 이름 | `{DOMAIN_UPPER}_{ACTION}` | `TEAM_NOT_FOUND`, `MATCH_ALREADY_FINISHED` |
| HttpStatus 매핑 | 의미와 일치 | (아래 표) |
| 메시지 | 한국어 평문, 사용자 안전 수준 | "팀을 찾을 수 없습니다" |
| 메시지에 절대 포함 X | PII, 내부 path, stack trace | (예: 사용자 이름·연락처·SQL) |

### HttpStatus 매핑 가이드

| 의미 | HttpStatus | 예시 ErrorCode |
|------|-----------|---------------|
| 리소스 없음 | 404 NOT_FOUND | `TEAM_NOT_FOUND` |
| 중복 / 상태 충돌 | 409 CONFLICT | `TEAM_ALREADY_EXISTS` |
| 입력 검증 실패 | 400 BAD_REQUEST | `INVALID_INPUT` |
| 권한 없음 (인증됨) | 403 FORBIDDEN | `NOT_TEAM_CAPTAIN` |
| 인증 안됨 | 401 UNAUTHORIZED | `TOKEN_INVALID` |
| 비즈니스 규칙 위반 | 422 UNPROCESSABLE_ENTITY | `LINEUP_INCOMPLETE` |
| 서버 내부 오류 | 500 INTERNAL_SERVER_ERROR | `EXTERNAL_API_FAILED` |

---

## 4. 위반 탐지 명령 (PR 전 자체 점검)

```bash
# 4-1. raw exception throw (있으면 위반)
grep -rEn 'throw\s+(RuntimeException|IllegalArgumentException|IllegalStateException|NullPointerException|Exception)\(' \
  dugout-api/src/main/kotlin \
  --include="*.kt" \
  | grep -v "BusinessException"

# 4-2. Controller에서 try/catch (있으면 위반 가능성 — 검토 필요)
grep -rEn 'try\s*\{' \
  dugout-api/src/main/kotlin/com/dugout/api/domain/*/controller \
  --include="*.kt"

# 4-3. ResponseEntity로 직접 에러 응답 (있으면 위반)
grep -rEn 'ResponseEntity\.(badRequest|status\(HttpStatus\.(BAD_REQUEST|NOT_FOUND|CONFLICT|FORBIDDEN|UNAUTHORIZED|INTERNAL_SERVER_ERROR))' \
  dugout-api/src/main/kotlin/com/dugout/api/domain \
  --include="*.kt"
```

세 명령 모두 **출력이 비어 있어야 통과**. 한 줄이라도 나오면 1·2번 가이드대로 수정.

---

## 5. PR 셀프 체크리스트

- [ ] 새/수정 service·controller에 raw exception throw가 없는가?
- [ ] Controller에 try/catch가 없는가? (검증 가드는 OK, 에러 응답 가공은 ❌)
- [ ] 새 비즈니스 에러는 `ErrorCode.kt`에 enum entry로 등록됐는가?
- [ ] `HttpStatus`가 3번 매핑 가이드와 일치하는가?
- [ ] 에러 메시지에 PII / 내부 path / stack trace가 노출되지 않는가?
- [ ] `GlobalExceptionHandler`가 새 `ErrorCode`를 자연스럽게 처리하는가? (대부분 자동)

---

## 6. 컴파일·검증

```bash
cd dugout-api && ./gradlew compileKotlin --quiet
```

빌드 통과 + 4번 grep 모두 0줄 = 통과.

---

## 7. 예외 (raw exception을 허용하는 경우)

다음 경우는 raw exception 사용이 정당하다:
- **테스트 코드** (`*Test.kt`) — 의도적인 실패 시나리오 검증
- **`global/`** 인프라 레이어 — 프레임워크 래퍼·필터에서 내부 신호 (예: `JwtFilter`의 `JwtException` 변환 전)
- **외부 라이브러리에서 throw 받아 BusinessException으로 즉시 변환**하는 어댑터

이 외 도메인 service / controller 레이어는 **무조건 BusinessException**.
