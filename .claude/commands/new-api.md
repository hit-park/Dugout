---
description: dugout-api에 새 백엔드 도메인 패키지를 스캐폴딩 (5폴더·6파일 생성 + ErrorCode 안내 + TDD.md 갱신 패치 제안)
argument-hint: <도메인명 (소문자 영문 단수형, 예: lineup / fee / mercenary)>
allowed-tools: Bash, Read, Edit, Write, Grep, Glob
---

사용자가 `/new-api $ARGUMENTS` 형태로 호출했다. 아래 절차를 **순서대로, 빠짐없이** 따른다. 자동 커밋·머지·푸시는 **절대 금지** — 사용자 검토 후 직접 결정.

---

## 0. 사전 검증 (위반 시 즉시 중단 + 안내)

- `$ARGUMENTS`가 비어있음 → 사용법 출력 후 중단
- 공백·대문자·특수문자 포함 → 소문자 영문 단수만 허용 안내
- 명백한 복수형(s/es로 끝남) → 단수형 사용 안내 (예: `lineups` ❌ → `lineup` ✅)
- `dugout-api/src/main/kotlin/com/dugout/api/domain/$ARGUMENTS/` 디렉토리 존재 → 덮어쓰기 방지로 중단

---

## 1. 명명 규칙 산출

`$ARGUMENTS` 기반으로 다음 변수를 결정한다 (이후 모든 단계에서 일관 사용):

| 변수 | 규칙 | lineup | mercenary | match |
|------|------|--------|-----------|-------|
| `{domain}` | 입력 그대로 | lineup | mercenary | match |
| `{Class}` | 첫 글자 대문자 | Lineup | Mercenary | Match |
| `{camelCase}` | 입력 그대로 (변수명) | lineup | mercenary | match |
| `{plural}` | 영문 복수형 | lineups | mercenaries | matches |
| `{TABLE}` | snake_case 복수형 | lineups | mercenaries | matches |
| `{UPPER}` | 대문자 (ErrorCode용) | LINEUP | MERCENARY | MATCH |

**복수형 변환 규칙**:
- 자음+y로 끝나면 → y를 ies로 (mercenary → mercenaries)
- s/x/z/ch/sh로 끝나면 → es 추가 (match → matches)
- 그 외 → s 추가 (lineup → lineups)

---

## 2. 디렉토리 5폴더 생성

`dugout-api/src/main/kotlin/com/dugout/api/domain/{domain}/` 아래:
- `controller/`
- `service/`
- `repository/`
- `entity/`
- `dto/`

---

## 3. 파일 6개 스캐폴딩

각 파일을 정확히 아래 템플릿대로 생성. `{Class}`, `{domain}`, `{plural}`, `{TABLE}`, `{camelCase}` 모두 1단계 산출값으로 치환.

### 3-1. Entity — `entity/{Class}.kt`

```kotlin
package com.dugout.api.domain.{domain}.entity

import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "{TABLE}")
class {Class}(
    // TODO: 도메인 필드 정의
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null
}
```

### 3-2. Repository — `repository/{Class}Repository.kt`

```kotlin
package com.dugout.api.domain.{domain}.repository

import com.dugout.api.domain.{domain}.entity.{Class}
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface {Class}Repository : JpaRepository<{Class}, Long>
```

### 3-3. Service — `service/{Class}Service.kt`

```kotlin
package com.dugout.api.domain.{domain}.service

import com.dugout.api.domain.{domain}.repository.{Class}Repository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class {Class}Service(
    private val {camelCase}Repository: {Class}Repository,
) {
    // TODO: 비즈니스 로직 구현
    // 비즈니스 에러는 throw BusinessException(ErrorCode.{UPPER}_NOT_FOUND)
    // raw RuntimeException / IllegalArgumentException 사용 금지
}
```

### 3-4. DTO Request — `dto/{Class}Request.kt`

```kotlin
package com.dugout.api.domain.{domain}.dto

import jakarta.validation.constraints.NotBlank

data class {Class}Request(
    @field:NotBlank(message = "필드값은 필수입니다")
    val placeholder: String,
    // TODO: 실제 필드로 교체
)
```

### 3-5. DTO Response — `dto/{Class}Response.kt`

```kotlin
package com.dugout.api.domain.{domain}.dto

data class {Class}Response(
    val id: Long,
    // TODO: 실제 필드 정의
)
```

### 3-6. Controller — `controller/{Class}Controller.kt`

```kotlin
package com.dugout.api.domain.{domain}.controller

import com.dugout.api.domain.{domain}.service.{Class}Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/{plural}")
class {Class}Controller(
    private val {camelCase}Service: {Class}Service,
) {
    // TODO: endpoint 정의
    // 페이징은 PageResponse<T> 사용 (com.dugout.api.global.common.PageResponse)
    // 요청 검증은 @Valid + Bean Validation
    // 응답은 항상 *Response DTO (Entity 직접 노출 금지)
}
```

---

## 4. ErrorCode 추가 안내 (자동 추가 ❌)

`global/error/ErrorCode.kt`를 Read 한 뒤, 다음 entry를 추가하는 안내를 사용자에게 출력한다 — 한글 메시지는 사용자가 도메인 의미에 맞게 결정.

```kotlin
{UPPER}_NOT_FOUND(HttpStatus.NOT_FOUND, "(한글 도메인명)을(를) 찾을 수 없습니다"),
```

도메인 특성에 따라 추가 후보를 함께 권유:
- `{UPPER}_ALREADY_EXISTS` (CONFLICT, 중복 생성 차단)
- `{UPPER}_INVALID` (BAD_REQUEST, 검증 실패)
- `{UPPER}_FORBIDDEN` (FORBIDDEN, 권한 없음)

---

## 5. 빠른 컴파일 점검

```bash
cd dugout-api && ./gradlew compileKotlin --quiet
```

성공이면 ✅ 보고. 실패면 출력 그대로 보고 후 중단 (대부분은 `BaseEntity` 또는 `ErrorCode` import 누락).

---

## 6. TDD.md 갱신 패치 제안 (자동 적용 ❌)

`docs/TDD.md`를 Read 한 뒤, 다음 두 섹션에 들어갈 패치를 **미리보기로만** 제안한다. 사용자 승인 후에만 Edit 적용.

### 6-1. "DB 스키마" 섹션 — 새 테이블

```markdown
#### `{TABLE}` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| created_at | TIMESTAMP | NOT NULL | BaseEntity |
| updated_at | TIMESTAMP | NOT NULL | BaseEntity |
| deleted_at | TIMESTAMP | NULL | 소프트 삭제 (BaseEntity) |
| (TODO) | | | |
```

### 6-2. "API 설계" 섹션 — 새 endpoint 그룹

```markdown
#### {Class} (`/api/v1/{plural}`)

| Method | Path | 설명 | 요청 | 응답 | ErrorCode |
|--------|------|------|------|------|-----------|
| (TODO) | | | `{Class}Request` | `{Class}Response` | `{UPPER}_NOT_FOUND` 등 |
```

---

## 7. 최종 결과 보고

작업 종료 후 다음 항목을 명시적으로 보고:

1. **생성된 파일 6개** — 절대경로 리스트 (마크다운 링크 형식)
2. **컴파일 결과** — ✅ 성공 / ❌ 실패 (실패 시 에러 출력)
3. **추가해야 할 ErrorCode** — 최소 1개 (`{UPPER}_NOT_FOUND`) + 권유 후보 3개
4. **TDD.md 갱신 미리보기 패치** — DB 스키마 + API 설계
5. **다음 단계 체크리스트**:
   - [ ] Entity 도메인 필드 정의
   - [ ] DTO 실제 필드 정의 (placeholder 제거)
   - [ ] Service 비즈니스 로직 구현
   - [ ] Controller endpoint 시그니처 작성
   - [ ] ErrorCode 한글 메시지 확정 후 `ErrorCode.kt` 편집
   - [ ] TDD.md 갱신 패치 승인 후 적용
   - [ ] 단위 테스트 작성

---

## 절대 금지 사항

- ❌ 자동 git add / commit / push / 머지
- ❌ ErrorCode.kt 자동 편집 (한글 메시지 결정 사용자 권한)
- ❌ TDD.md 자동 편집 (사용자 승인 후 패치)
- ❌ 도메인 필드를 임의로 추측해서 채워넣기 (placeholder 유지)
