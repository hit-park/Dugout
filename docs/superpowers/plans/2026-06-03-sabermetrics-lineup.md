# 세이버매트릭스 타순 추천 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 타석 단위(L2) 경기 기록을 수집하고, 그 기록으로 보정 OBP/SLG/ISO를 계산해 "The Book lite" 규칙으로 라인업 타순을 추천한다.

**Architecture:** dugout-api는 타석 기록 CRUD + raw 카운트 집계만 담당하고(데이터 주인), dugout-ai가 shrinkage 보정·타순 결정을 전담한다(알고리즘 주인). 수비 배치는 기존 헝가리안 그대로 두고 타순 레이어만 세이버매트릭스화한다. 기록이 없으면 기존 좌우타 교차 로직으로 폴백한다.

**Tech Stack:** Kotlin + Spring Boot 3 (JPA, JUnit5 + mockito-kotlin, H2), Python 3.12 + FastAPI (Pydantic v2, pytest, scipy).

**설계 문서:** `docs/superpowers/specs/2026-06-03-sabermetrics-lineup-design.md`

**선행 결정 (코드 작성 전 확인):**
- `BaseEntity`에는 `deletedAt`이 없다 → 타석 기록 삭제는 **hard delete**.
- ErrorCode는 기존 것 재사용: 경기 없음 → `MATCH_NOT_FOUND`, 팀 멤버 아님 → `NOT_TEAM_MEMBER`, 잘못된 입력 → `INVALID_INPUT`. 신규는 `RECORD_NOT_FOUND` 하나만 추가.
- 타석 기록 API 경로는 `/api/v1/records/*` (도메인 복수형 컨벤션).
- 세이버매트릭스 "판단"(shrinkage k, 슬로팅 규칙)은 dugout-ai에만. dugout-api의 batting-stats 응답은 표시용 기본 지표(AVG/OBP/SLG/OPS)까지만 계산.

---

## File Structure

### dugout-api (신규 `domain/record/`)
- Create: `domain/record/entity/BattingResult.kt` — 타격 결과 enum
- Create: `domain/record/entity/PlateAppearance.kt` — 타석 기록 엔티티
- Create: `domain/record/repository/PlateAppearanceRepository.kt`
- Create: `domain/record/dto/RecordDto.kt` — Request/Response DTO
- Create: `domain/record/service/RecordService.kt` — CRUD + 집계
- Create: `domain/record/controller/RecordController.kt`
- Modify: `global/error/ErrorCode.kt` — `RECORD_NOT_FOUND` 추가
- Modify: `global/ai/AiDtos.kt` — `AiAttendeeProfile`에 타격 카운트 필드 추가
- Modify: `domain/lineup/service/LineupService.kt` — 출석자별 카운트 집계 후 AI 요청에 포함
- Test: `domain/record/service/RecordServiceTest.kt`, `domain/record/controller/RecordControllerTest.kt`

### dugout-ai
- Modify: `app/schemas/lineup.py` — `AttendeeProfile`에 타격 카운트 필드 추가
- Create: `app/services/batting_order.py` — 보정 지표 + The Book lite 슬로팅
- Modify: `app/services/hungarian.py` — 타순 단계에서 batting_order 호출, 폴백 유지
- Test: `tests/test_batting_order.py`

---

## Milestone A — dugout-api 타석 기록 수집

작업 디렉토리: `dugout-api/`. 모든 명령은 `dugout-api/`에서 실행.

### Task A1: BattingResult enum + PlateAppearance 엔티티

**Files:**
- Create: `src/main/kotlin/com/dugout/api/domain/record/entity/BattingResult.kt`
- Create: `src/main/kotlin/com/dugout/api/domain/record/entity/PlateAppearance.kt`

- [ ] **Step 1: BattingResult enum 작성**

```kotlin
package com.dugout.api.domain.record.entity

enum class BattingResult {
    SINGLE, DOUBLE, TRIPLE, HOME_RUN,   // 안타류
    WALK, HIT_BY_PITCH,                 // 출루 (타수 제외)
    SACRIFICE_FLY,                      // 희생플라이
    STRIKEOUT, IN_PLAY_OUT,             // 아웃
    REACHED_ON_ERROR,                   // 실책출루 (OBP/SLG 계산 시 아웃 취급)
}
```

- [ ] **Step 2: PlateAppearance 엔티티 작성**

`LineupEntry.kt` 패턴(ManyToOne LAZY + BaseEntity)을 따른다.

```kotlin
package com.dugout.api.domain.record.entity

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "plate_appearances")
class PlateAppearance(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    val match: Match,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id", nullable = false)
    val teamMember: TeamMember,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val result: BattingResult,

    @Column(nullable = false)
    val rbi: Int = 0,
) : BaseEntity()
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew compileKotlin --quiet`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/record/entity/
git commit -m "feat(record): 타석 기록 엔티티 PlateAppearance + BattingResult enum 추가"
```

### Task A2: ErrorCode 추가

**Files:**
- Modify: `src/main/kotlin/com/dugout/api/global/error/ErrorCode.kt:88-89`

- [ ] **Step 1: RECORD_NOT_FOUND 추가**

`// Notification` 섹션 바로 앞(라인 88, `// AI Service` 블록 다음)에 추가:

```kotlin
    // Record
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "타석 기록을 찾을 수 없습니다"),
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileKotlin --quiet`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dugout/api/global/error/ErrorCode.kt
git commit -m "feat(record): ErrorCode RECORD_NOT_FOUND 추가"
```

### Task A3: PlateAppearanceRepository

**Files:**
- Create: `src/main/kotlin/com/dugout/api/domain/record/repository/PlateAppearanceRepository.kt`

- [ ] **Step 1: Repository 작성**

`LineupEntryRepository` 패턴(파생 쿼리)을 따른다. 아마추어 데이터량이 작아 행을 모두 가져와 서비스에서 in-memory 집계한다.

```kotlin
package com.dugout.api.domain.record.repository

import com.dugout.api.domain.record.entity.PlateAppearance
import org.springframework.data.jpa.repository.JpaRepository

interface PlateAppearanceRepository : JpaRepository<PlateAppearance, Long> {
    fun findByMatchId(matchId: Long): List<PlateAppearance>
    fun findByTeamMemberIdIn(teamMemberIds: Collection<Long>): List<PlateAppearance>
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileKotlin --quiet`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/record/repository/
git commit -m "feat(record): PlateAppearanceRepository 추가"
```

### Task A4: Record DTO

**Files:**
- Create: `src/main/kotlin/com/dugout/api/domain/record/dto/RecordDto.kt`

- [ ] **Step 1: DTO 작성**

`*Request`/`*Response` 분리 + Bean Validation. `BattingStats`는 raw 카운트 + 표시용 기본 지표(AVG/OBP/SLG/OPS).

```kotlin
package com.dugout.api.domain.record.dto

import com.dugout.api.domain.record.entity.BattingResult
import com.dugout.api.domain.record.entity.PlateAppearance
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreatePlateAppearanceRequest(
    @field:NotNull val matchId: Long,
    @field:NotNull val teamMemberId: Long,
    @field:NotNull val result: BattingResult,
    @field:Min(0) val rbi: Int = 0,
)

data class PlateAppearanceResponse(
    val id: Long,
    val matchId: Long,
    val teamMemberId: Long,
    val result: BattingResult,
    val rbi: Int,
) {
    companion object {
        fun of(pa: PlateAppearance) = PlateAppearanceResponse(
            id = pa.id,
            matchId = pa.match.id,
            teamMemberId = pa.teamMember.id,
            result = pa.result,
            rbi = pa.rbi,
        )
    }
}

data class BattingStatsResponse(
    val teamMemberId: Long,
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val avg: Double,
    val obp: Double,
    val slg: Double,
    val ops: Double,
)
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileKotlin --quiet`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/record/dto/
git commit -m "feat(record): 타석 기록 DTO 추가"
```

### Task A5: RecordService — 기록 생성/조회/삭제 (TDD)

**Files:**
- Create: `src/test/kotlin/com/dugout/api/domain/record/service/RecordServiceTest.kt`
- Create: `src/main/kotlin/com/dugout/api/domain/record/service/RecordService.kt`

- [ ] **Step 1: 실패 테스트 작성**

`LineupServiceTest` 패턴(mockito-kotlin)을 따른다. 검증: 경기 없으면 MATCH_NOT_FOUND, 팀 멤버 아니면 NOT_TEAM_MEMBER, 정상 생성.

```kotlin
package com.dugout.api.domain.record.service

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.entity.BattingResult
import com.dugout.api.domain.record.entity.PlateAppearance
import com.dugout.api.domain.record.repository.PlateAppearanceRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class RecordServiceTest {
    private val plateAppearanceRepository = mock<PlateAppearanceRepository>()
    private val matchRepository = mock<MatchRepository>()
    private val teamMemberRepository = mock<TeamMemberRepository>()
    private val service = RecordService(plateAppearanceRepository, matchRepository, teamMemberRepository)

    private fun match(teamId: Long = 10L): Match {
        val team = mock<Team> { whenever(it.id).thenReturn(teamId) }
        return mock { whenever(it.team).thenReturn(team) }
    }

    @Test
    fun `경기가 없으면 MATCH_NOT_FOUND`() {
        whenever(matchRepository.findById(1L)).thenReturn(Optional.empty())
        val req = CreatePlateAppearanceRequest(matchId = 1L, teamMemberId = 2L, result = BattingResult.SINGLE)
        val ex = assertThrows(BusinessException::class.java) { service.create(userId = 99L, request = req) }
        assertEquals(ErrorCode.MATCH_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `요청자가 팀 멤버가 아니면 NOT_TEAM_MEMBER`() {
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(match(teamId = 10L)))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(10L, 99L)).thenReturn(false)
        val req = CreatePlateAppearanceRequest(matchId = 1L, teamMemberId = 2L, result = BattingResult.SINGLE)
        val ex = assertThrows(BusinessException::class.java) { service.create(userId = 99L, request = req) }
        assertEquals(ErrorCode.NOT_TEAM_MEMBER, ex.errorCode)
    }

    @Test
    fun `정상 생성 시 저장된 응답 반환`() {
        val m = match(teamId = 10L).also { whenever(it.id).thenReturn(1L) }
        val member = mock<TeamMember> { whenever(it.id).thenReturn(2L) }
        whenever(matchRepository.findById(1L)).thenReturn(Optional.of(m))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(10L, 99L)).thenReturn(true)
        whenever(teamMemberRepository.findById(2L)).thenReturn(Optional.of(member))
        whenever(plateAppearanceRepository.save(any<PlateAppearance>()))
            .thenAnswer { it.arguments[0] as PlateAppearance }

        val req = CreatePlateAppearanceRequest(matchId = 1L, teamMemberId = 2L, result = BattingResult.DOUBLE, rbi = 1)
        val res = service.create(userId = 99L, request = req)

        assertEquals(BattingResult.DOUBLE, res.result)
        assertEquals(1, res.rbi)
    }
}
```

> 주: `BusinessException`이 `errorCode` 프로퍼티를 노출하는지 확인. 다르면(`code` 등) 해당 이름으로 맞춘다 — `global/error/BusinessException.kt`를 먼저 읽을 것.

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.dugout.api.domain.record.service.RecordServiceTest" --quiet`
Expected: FAIL — `RecordService` 미존재(컴파일 에러)

- [ ] **Step 3: RecordService 구현**

```kotlin
package com.dugout.api.domain.record.service

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.dto.PlateAppearanceResponse
import com.dugout.api.domain.record.entity.PlateAppearance
import com.dugout.api.domain.record.repository.PlateAppearanceRepository
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RecordService(
    private val plateAppearanceRepository: PlateAppearanceRepository,
    private val matchRepository: MatchRepository,
    private val teamMemberRepository: TeamMemberRepository,
) {

    @Transactional
    fun create(userId: Long, request: CreatePlateAppearanceRequest): PlateAppearanceResponse {
        val match = findMatch(request.matchId)
        requireTeamMember(match.team.id, userId)
        val teamMember = teamMemberRepository.findById(request.teamMemberId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND) }

        val saved = plateAppearanceRepository.save(
            PlateAppearance(
                match = match,
                teamMember = teamMember,
                result = request.result,
                rbi = request.rbi,
            ),
        )
        return PlateAppearanceResponse.of(saved)
    }

    fun listByMatch(userId: Long, matchId: Long): List<PlateAppearanceResponse> {
        val match = findMatch(matchId)
        requireTeamMember(match.team.id, userId)
        return plateAppearanceRepository.findByMatchId(matchId).map(PlateAppearanceResponse::of)
    }

    @Transactional
    fun delete(userId: Long, recordId: Long) {
        val pa = plateAppearanceRepository.findById(recordId)
            .orElseThrow { BusinessException(ErrorCode.RECORD_NOT_FOUND) }
        requireTeamMember(pa.match.team.id, userId)
        plateAppearanceRepository.delete(pa)
    }

    private fun findMatch(matchId: Long): Match =
        matchRepository.findById(matchId)
            .orElseThrow { BusinessException(ErrorCode.MATCH_NOT_FOUND) }

    private fun requireTeamMember(teamId: Long, userId: Long) {
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId)) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.dugout.api.domain.record.service.RecordServiceTest" --quiet`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/record/service/ src/test/kotlin/com/dugout/api/domain/record/service/
git commit -m "feat(record): 타석 기록 생성/조회/삭제 RecordService (TDD)"
```

### Task A6: RecordService — 타격 집계 (TDD)

**Files:**
- Modify: `src/test/kotlin/com/dugout/api/domain/record/service/RecordServiceTest.kt`
- Modify: `src/main/kotlin/com/dugout/api/domain/record/service/RecordService.kt`

- [ ] **Step 1: 실패 테스트 추가**

집계 산식 검증: 단타1·2루타1·볼넷1·삼진1 → PA4, AB3, H2, AVG .667, OBP .750, SLG 1.000.

```kotlin
    @Test
    fun `타격 집계 산식 검증`() {
        // 활성 팀멤버 1명, 그의 타석 4개
        val member = mock<TeamMember> { whenever(it.id).thenReturn(2L) }
        whenever(teamMemberRepository.findByTeamIdAndIsActiveTrue(10L)).thenReturn(listOf(member))
        whenever(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(10L, 99L)).thenReturn(true)

        fun pa(r: BattingResult) = mock<PlateAppearance> {
            whenever(it.teamMember).thenReturn(member)
            whenever(it.result).thenReturn(r)
        }
        whenever(plateAppearanceRepository.findByTeamMemberIdIn(listOf(2L))).thenReturn(
            listOf(pa(BattingResult.SINGLE), pa(BattingResult.DOUBLE), pa(BattingResult.WALK), pa(BattingResult.STRIKEOUT)),
        )

        val stats = service.battingStats(userId = 99L, teamId = 10L).single()
        assertEquals(4, stats.plateAppearances)
        assertEquals(3, stats.atBats)
        assertEquals(2, stats.hits)
        assertEquals(0.667, stats.avg, 0.001)   // 2/3
        assertEquals(0.750, stats.obp, 0.001)   // (2+1)/(3+1)
        assertEquals(1.000, stats.slg, 0.001)   // (1+2)/3
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.dugout.api.domain.record.service.RecordServiceTest" --quiet`
Expected: FAIL — `battingStats` 미존재

- [ ] **Step 3: battingStats 구현 (RecordService에 추가)**

```kotlin
    fun battingStats(userId: Long, teamId: Long): List<BattingStatsResponse> {
        requireTeamMember(teamId, userId)
        val memberIds = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId).map { it.id }
        if (memberIds.isEmpty()) return emptyList()

        return plateAppearanceRepository.findByTeamMemberIdIn(memberIds)
            .groupBy { it.teamMember.id }
            .map { (memberId, list) -> aggregate(memberId, list) }
    }

    private fun aggregate(memberId: Long, list: List<PlateAppearance>): BattingStatsResponse {
        fun count(r: BattingResult) = list.count { it.result == r }
        val singles = count(BattingResult.SINGLE)
        val doubles = count(BattingResult.DOUBLE)
        val triples = count(BattingResult.TRIPLE)
        val hr = count(BattingResult.HOME_RUN)
        val bb = count(BattingResult.WALK)
        val hbp = count(BattingResult.HIT_BY_PITCH)
        val sf = count(BattingResult.SACRIFICE_FLY)

        val pa = list.size
        val hits = singles + doubles + triples + hr
        val ab = pa - bb - hbp - sf                      // ROE·삼진·인플레이아웃은 AB에 포함
        val totalBases = singles + 2 * doubles + 3 * triples + 4 * hr
        val obpDenom = ab + bb + hbp + sf

        fun ratio(num: Int, den: Int) = if (den == 0) 0.0 else num.toDouble() / den
        val avg = ratio(hits, ab)
        val obp = ratio(hits + bb + hbp, obpDenom)
        val slg = ratio(totalBases, ab)

        return BattingStatsResponse(
            teamMemberId = memberId,
            plateAppearances = pa,
            atBats = ab,
            hits = hits,
            avg = avg,
            obp = obp,
            slg = slg,
            ops = obp + slg,
        )
    }
```

`BattingStatsResponse`, `BattingResult` import 추가.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.dugout.api.domain.record.service.RecordServiceTest" --quiet`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/record/service/RecordService.kt src/test/kotlin/com/dugout/api/domain/record/service/RecordServiceTest.kt
git commit -m "feat(record): 선수별 타격 집계 battingStats (TDD)"
```

### Task A7: RecordController + 통합 테스트

**Files:**
- Create: `src/main/kotlin/com/dugout/api/domain/record/controller/RecordController.kt`

- [ ] **Step 1: Controller 작성**

`LineupController` 패턴(`@AuthenticationPrincipal userId: Long`, ResponseEntity, /api/v1).

```kotlin
package com.dugout.api.domain.record.controller

import com.dugout.api.domain.record.dto.BattingStatsResponse
import com.dugout.api.domain.record.dto.CreatePlateAppearanceRequest
import com.dugout.api.domain.record.dto.PlateAppearanceResponse
import com.dugout.api.domain.record.service.RecordService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val recordService: RecordService,
) {

    @PostMapping("/plate-appearances")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreatePlateAppearanceRequest,
    ): ResponseEntity<PlateAppearanceResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(recordService.create(userId, request))

    @GetMapping("/plate-appearances")
    fun listByMatch(
        @AuthenticationPrincipal userId: Long,
        @RequestParam matchId: Long,
    ): ResponseEntity<List<PlateAppearanceResponse>> =
        ResponseEntity.ok(recordService.listByMatch(userId, matchId))

    @DeleteMapping("/plate-appearances/{recordId}")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable recordId: Long,
    ): ResponseEntity<Void> {
        recordService.delete(userId, recordId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/teams/{teamId}/batting-stats")
    fun battingStats(
        @AuthenticationPrincipal userId: Long,
        @PathVariable teamId: Long,
    ): ResponseEntity<List<BattingStatsResponse>> =
        ResponseEntity.ok(recordService.battingStats(userId, teamId))
}
```

- [ ] **Step 2: SecurityConfig 인증 규칙 확인**

`global/config/SecurityConfig.kt`를 읽고 `/api/v1/records/**`가 인증 필요 경로에 포함되는지 확인. 다른 도메인(`/api/v1/matches/**` 등)과 동일 정책이면 별도 수정 불필요. permitAll 화이트리스트에 신규 경로를 넣지 말 것.

- [ ] **Step 3: 전체 컴파일 + 테스트**

Run: `./gradlew compileKotlin compileTestKotlin --quiet && ./gradlew test --tests "com.dugout.api.domain.record.*" --quiet`
Expected: BUILD SUCCESSFUL, 테스트 PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/record/controller/
git commit -m "feat(record): RecordController (/api/v1/records) 추가"
```

---

## Milestone B — dugout-ai 세이버매트릭스 엔진

작업 디렉토리: `dugout-ai/`. 가상환경 활성화 후 진행.

### Task B1: AttendeeProfile 스키마에 타격 카운트 추가

**Files:**
- Modify: `app/schemas/lineup.py:4-11`

- [ ] **Step 1: AttendeeProfile에 카운트 필드 추가**

기존 필드 아래에 추가. 전부 기본값 0이라 기존 호출은 영향 없음(폴백 동작 유지).

```python
class AttendeeProfile(BaseModel):
    """라인업 추천 입력 - 출석자 1명의 프로필."""

    user_id: int
    primary_position: str            # 주포지션 (P/C/1B/2B/3B/SS/LF/CF/RF/DH)
    sub_positions: list[str] = Field(default_factory=list)
    bench_ratio_recent: float = Field(ge=0.0, le=1.0, default=0.0, description="최근 N경기 벤치 비율")
    bats_left: bool = False          # 좌타 여부 (타순 좌우 교차용)

    # 타석 기록(L2) raw 카운트 — 전부 0이면 콜드 스타트로 간주해 좌우타 교차 폴백
    singles: int = 0
    doubles: int = 0
    triples: int = 0
    home_runs: int = 0
    walks: int = 0
    hit_by_pitch: int = 0
    sacrifice_flies: int = 0
    strikeouts: int = 0
    in_play_outs: int = 0
    reached_on_errors: int = 0
```

- [ ] **Step 2: 기존 테스트 회귀 확인**

Run: `pytest tests/test_lineup.py -q`
Expected: PASS (기존 4 tests — 기본값 0이라 동작 불변)

- [ ] **Step 3: Commit**

```bash
git add app/schemas/lineup.py
git commit -m "feat(lineup): AttendeeProfile에 타석 기록 카운트 필드 추가"
```

### Task B2: batting_order.py — 보정 지표 + shrinkage (TDD)

**Files:**
- Create: `tests/test_batting_order.py`
- Create: `app/services/batting_order.py`

- [ ] **Step 1: 실패 테스트 작성**

```python
from app.schemas.lineup import AttendeeProfile
from app.services import batting_order


def _player(uid: int, **counts) -> AttendeeProfile:
    return AttendeeProfile(user_id=uid, primary_position="DH", **counts)


def test_components_basic():
    # 단타1 2루타1 볼넷1 삼진1 → PA4 AB3 H2 OBP .750 SLG 1.000
    p = _player(1, singles=1, doubles=1, walks=1, strikeouts=1)
    c = batting_order._components(p)
    assert c.pa == 4
    assert c.ab == 3
    assert c.hits == 2
    assert abs(c.on_base - 3) < 1e-9          # 2안타 + 1볼넷
    assert abs(c.obp_denom - 4) < 1e-9        # AB3 + BB1
    assert c.total_bases == 3                 # 1 + 2


def test_has_records_false_when_all_zero():
    assert batting_order.has_records([_player(1), _player(2)]) is False


def test_has_records_true_when_any_pa():
    assert batting_order.has_records([_player(1), _player(2, singles=1)]) is True


def test_shrinkage_pulls_small_sample_toward_team_mean():
    # 5타석 .800 OBP 선수 vs 팀평균 .300 → 보정 OBP가 팀평균 쪽으로 크게 수축
    small = _player(1, singles=4, in_play_outs=1)   # 5타석 4안타 → raw OBP .800
    team_obp, team_iso = 0.300, 0.100
    adj_obp, _ = batting_order._adjusted(small, team_obp, team_iso)
    assert adj_obp < 0.500                          # k=50이라 5타석은 평균 쪽으로 강하게 수축
    assert adj_obp > team_obp                        # 그래도 평균보다는 높음
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pytest tests/test_batting_order.py -q`
Expected: FAIL — `app.services.batting_order` 미존재

- [ ] **Step 3: batting_order.py 구현 (지표 + shrinkage 부분)**

```python
"""세이버매트릭스 타순 엔진 (The Book lite + shrinkage). TDD 3-3-1.

- 보정 OBP/SLG/ISO 계산 (실책출루 ROE는 아웃 취급)
- 작은 표본 노이즈는 평균 회귀(shrinkage, k=50)로 보정
- The Book lite 슬로팅: 2번에 종합 최고타자, 1번 출루형, 4번 장타형
- 콜드 스타트(기록 0): None 반환 → 호출 측이 좌우타 교차로 폴백
"""

from dataclasses import dataclass

from app.schemas.lineup import AttendeeProfile

K_SHRINKAGE = 50  # 가상 타석 — 표본이 작을수록 팀 평균으로 수축


@dataclass(frozen=True)
class Components:
    pa: int
    ab: int
    hits: int
    on_base: int
    obp_denom: int
    total_bases: int


def _components(a: AttendeeProfile) -> Components:
    hits = a.singles + a.doubles + a.triples + a.home_runs
    pa = (
        hits
        + a.walks + a.hit_by_pitch + a.sacrifice_flies
        + a.strikeouts + a.in_play_outs + a.reached_on_errors
    )
    ab = pa - a.walks - a.hit_by_pitch - a.sacrifice_flies
    on_base = hits + a.walks + a.hit_by_pitch
    obp_denom = ab + a.walks + a.hit_by_pitch + a.sacrifice_flies
    total_bases = a.singles + 2 * a.doubles + 3 * a.triples + 4 * a.home_runs
    return Components(pa, ab, hits, on_base, obp_denom, total_bases)


def has_records(attendees: list[AttendeeProfile]) -> bool:
    return any(_components(a).pa > 0 for a in attendees)


def _team_averages(attendees: list[AttendeeProfile]) -> tuple[float, float]:
    total_on_base = total_obp_denom = total_tb = total_hits = total_ab = 0
    for a in attendees:
        c = _components(a)
        total_on_base += c.on_base
        total_obp_denom += c.obp_denom
        total_tb += c.total_bases
        total_hits += c.hits
        total_ab += c.ab
    team_obp = total_on_base / total_obp_denom if total_obp_denom else 0.0
    team_iso = (total_tb - total_hits) / total_ab if total_ab else 0.0
    return team_obp, team_iso


def _adjusted(a: AttendeeProfile, team_obp: float, team_iso: float) -> tuple[float, float]:
    c = _components(a)
    adj_obp = (c.on_base + team_obp * K_SHRINKAGE) / (c.obp_denom + K_SHRINKAGE)
    raw_iso = (c.total_bases - c.hits) / c.ab if c.ab else 0.0
    adj_iso = (raw_iso * c.pa + team_iso * K_SHRINKAGE) / (c.pa + K_SHRINKAGE)
    return adj_obp, adj_iso
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pytest tests/test_batting_order.py -q`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/services/batting_order.py tests/test_batting_order.py
git commit -m "feat(lineup): 세이버매트릭스 보정 지표 + shrinkage (TDD)"
```

### Task B3: batting_order.py — The Book lite 슬로팅 (TDD)

**Files:**
- Modify: `tests/test_batting_order.py`
- Modify: `app/services/batting_order.py`

- [ ] **Step 1: 실패 테스트 추가**

```python
def test_order_puts_best_overall_at_second_and_power_at_fourth():
    # 9명: 한 명은 종합 최고(고출루+고장타), 한 명은 순수 장타, 한 명은 순수 출루
    best = _player(1, singles=40, doubles=20, home_runs=20, walks=40)   # 고OBP+고ISO
    power = _player(2, home_runs=40, strikeouts=60)                     # 고ISO 저OBP
    onbase = _player(3, singles=30, walks=60, in_play_outs=30)          # 고OBP 저ISO
    fillers = [_player(i, singles=10, in_play_outs=40) for i in range(4, 10)]
    order = batting_order.order([best, power, onbase, *fillers])

    assert order is not None
    assert order[1] == 2          # 종합 최고타자 → 2번 (The Book 반전)
    assert order[2] == 4          # 순수 장타 → 4번
    assert order[3] == 1          # 순수 출루 → 1번


def test_order_returns_none_on_cold_start():
    cold = [_player(i) for i in range(1, 10)]   # 전원 기록 0
    assert batting_order.order(cold) is None
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pytest tests/test_batting_order.py -q`
Expected: FAIL — `order` 미존재

- [ ] **Step 3: order() 구현 (batting_order.py에 추가)**

```python
def order(starters: list[AttendeeProfile]) -> dict[int, int] | None:
    """선발 9명의 타순(user_id -> 1..9)을 반환. 기록 없으면 None(폴백 신호)."""
    if not has_records(starters):
        return None

    team_obp, team_iso = _team_averages(starters)
    adj = {
        s.user_id: _adjusted(s, team_obp, team_iso)
        for s in starters
    }

    def leadoff(uid: int) -> float:
        return adj[uid][0]                          # adj_obp

    def cleanup(uid: int) -> float:
        obp, iso = adj[uid]
        return 0.7 * iso + 0.3 * obp

    def overall(uid: int) -> float:
        obp, iso = adj[uid]
        return obp + 0.5 * iso

    remaining = [s.user_id for s in starters]
    slot_of: dict[int, int] = {}

    def take(slot: int, scorer) -> None:
        chosen = max(remaining, key=scorer)
        remaining.remove(chosen)
        slot_of[chosen] = slot

    take(2, overall)    # The Book 반전: 2번에 종합 최고타자
    take(1, leadoff)    # 순수 출루형
    take(4, cleanup)    # 최고 장타
    take(3, overall)
    take(5, cleanup)
    for i, uid in enumerate(sorted(remaining, key=leadoff, reverse=True)):
        slot_of[uid] = 6 + i        # 6~9번: adj_OBP 내림차순

    return slot_of
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pytest tests/test_batting_order.py -q`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add app/services/batting_order.py tests/test_batting_order.py
git commit -m "feat(lineup): The Book lite 타순 슬로팅 (TDD)"
```

### Task B4: hungarian.py 통합 — 기록 있으면 세이버매트릭스, 없으면 폴백

**Files:**
- Modify: `app/services/hungarian.py:43-49`
- Modify: `tests/test_batting_order.py`

- [ ] **Step 1: hungarian.py 타순 단계 교체**

기존 라인 43-49(타순 계산)를 아래로 교체. `_interleave_batting_order` 폴백은 유지.

```python
    # 타순: 기록 있으면 세이버매트릭스(The Book lite), 없으면 좌우타 교차 폴백
    from app.services import batting_order

    starter_users = [req.attendees[idx] for _, idx in starters_sorted_by_position]
    sabermetric_order = batting_order.order(starter_users)

    if sabermetric_order is None:
        batting = _interleave_batting_order(starter_users)
        order_of = {uid: batting.index(uid) + 1 for uid in batting}
    else:
        order_of = sabermetric_order
```

그리고 아래 for 루프(라인 47-57)에서 `order = batting_order.index(attendee.user_id) + 1`를 `order = order_of[attendee.user_id]`로 변경.

> 주: import는 파일 상단으로 올려도 무방하나 순환 import 방지를 위해 함수 내 지연 import 사용. 모듈명이 지역 변수 `batting_order`(없음)와 충돌하지 않도록 상단 import 권장: `from app.services import batting_order as batting_engine` 후 호출부도 일치시킬 것.

- [ ] **Step 2: 통합 회귀 테스트 추가 (tests/test_batting_order.py 또는 test_lineup.py)**

기록이 실린 요청이 200을 반환하고 2번에 종합 최고타자가 오는지 라우터 레벨로 확인.

```python
from fastapi.testclient import TestClient

from app.main import app

_client = TestClient(app)


def test_recommend_uses_sabermetric_order_when_records_present():
    attendees = []
    for i in range(9):
        a = {"user_id": i + 1, "primary_position": ["P","C","1B","2B","3B","SS","LF","CF","RF"][i],
             "sub_positions": [], "bench_ratio_recent": 0.0, "bats_left": False,
             "singles": 10, "in_play_outs": 40}
        attendees.append(a)
    # user_id 1을 종합 최고타자로
    attendees[0].update({"singles": 40, "doubles": 20, "home_runs": 20, "walks": 40, "in_play_outs": 0})

    res = _client.post("/api/lineups/recommend",
                       json={"match_id": 1, "attendees": attendees, "lineup_mode": "COMPETITIVE"})
    assert res.status_code == 200
    entries = {e["user_id"]: e["batting_order"] for e in res.json()["entries"] if not e["is_bench"]}
    assert entries[1] == 2     # 종합 최고타자 → 2번
```

- [ ] **Step 3: 전체 테스트 + 린트 + 타입체크**

Run: `pytest -q && ruff check . && mypy app/`
Expected: 전체 PASS (기존 test_lineup 4 + batting_order 7 포함), ruff/mypy 무오류

- [ ] **Step 4: Commit**

```bash
git add app/services/hungarian.py tests/
git commit -m "feat(lineup): 타순 단계에 세이버매트릭스 엔진 통합 + 콜드스타트 폴백"
```

---

## Milestone C — 통합 (API → AI 타격 카운트 전달)

작업 디렉토리: `dugout-api/`.

### Task C1: AiAttendeeProfile에 타격 카운트 필드 추가

**Files:**
- Modify: `src/main/kotlin/com/dugout/api/global/ai/AiDtos.kt` (`AiAttendeeProfile`)

- [ ] **Step 1: 필드 추가**

Python `AttendeeProfile`과 1:1 매칭(snake_case는 jackson naming strategy로 자동 매핑). 전부 기본값 0.

```kotlin
data class AiAttendeeProfile(
    val userId: Long,
    val primaryPosition: String,
    val subPositions: List<String> = emptyList(),
    val benchRatioRecent: Double = 0.0,
    val batsLeft: Boolean = false,
    val singles: Int = 0,
    val doubles: Int = 0,
    val triples: Int = 0,
    val homeRuns: Int = 0,
    val walks: Int = 0,
    val hitByPitch: Int = 0,
    val sacrificeFlies: Int = 0,
    val strikeouts: Int = 0,
    val inPlayOuts: Int = 0,
    val reachedOnErrors: Int = 0,
)
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileKotlin --quiet`
Expected: BUILD SUCCESSFUL (기본값이라 기존 호출부 영향 없음)

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dugout/api/global/ai/AiDtos.kt
git commit -m "feat(lineup): AiAttendeeProfile에 타격 카운트 필드 추가"
```

### Task C2: LineupService — 출석자별 카운트 집계 후 AI 요청에 포함 (TDD)

**Files:**
- Modify: `src/main/kotlin/com/dugout/api/domain/lineup/service/LineupService.kt`
- Test: `src/test/kotlin/com/dugout/api/domain/lineup/service/LineupServiceTest.kt`

- [ ] **Step 1: 실패 테스트 추가**

`recommend` 호출 시 AI 요청의 attendee 카운트가 해당 출석자의 타석 기록으로 채워지는지 검증(`argumentCaptor`). 기존 `LineupServiceTest`의 셋업(mock 출석자·match)을 재사용하되, `PlateAppearanceRepository` mock을 주입한다.

```kotlin
    @Test
    fun `recommend는 출석자의 타석 기록을 집계해 AI 요청에 싣는다`() {
        // 출석자 9명 셋업은 기존 헬퍼 사용. user_id=1 출석자에 대응하는 TeamMember(id=201)에 단타 2개 기록.
        // (기존 테스트의 match/attendees mock 구성을 따른다)
        whenever(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 1L))
            .thenReturn(mock { whenever(it.id).thenReturn(201L) })
        whenever(plateAppearanceRepository.findByTeamMemberIdIn(any())).thenReturn(
            listOf(
                mock { whenever(it.teamMember).thenReturn(mock { m -> whenever(m.id).thenReturn(201L) })
                       whenever(it.result).thenReturn(BattingResult.SINGLE) },
                mock { whenever(it.teamMember).thenReturn(mock { m -> whenever(m.id).thenReturn(201L) })
                       whenever(it.result).thenReturn(BattingResult.SINGLE) },
            ),
        )
        val captor = argumentCaptor<AiLineupRecommendRequest>()
        whenever(dugoutAiClient.recommendLineup(captor.capture())).thenReturn(/* 기존 stub 응답 */ aiStubResponse())

        service.recommend(userId = CAPTAIN_ID, matchId = MATCH_ID)

        val profile = captor.firstValue.attendees.first { it.userId == 1L }
        assertEquals(2, profile.singles)
    }
```

> 주: 기존 `LineupServiceTest`의 상수(TEAM_ID/MATCH_ID/CAPTAIN_ID)와 stub 응답 헬퍼 이름에 맞춰 조정한다. 파일을 먼저 읽고 셋업 패턴을 그대로 재사용할 것.

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.dugout.api.domain.lineup.service.LineupServiceTest" --quiet`
Expected: FAIL — 컴파일 에러(`plateAppearanceRepository` 미주입) 또는 카운트 0

- [ ] **Step 3: LineupService 구현 변경**

생성자에 `PlateAppearanceRepository` 주입. `recommend`에서 출석자 userId → teamMember 매핑 후 카운트 집계.

```kotlin
// 생성자 파라미터에 추가
    private val plateAppearanceRepository: PlateAppearanceRepository,
```

```kotlin
// recommend() 내부, AI 요청 빌드 부분을 교체
        val teamId = match.team.id
        val memberByUser: Map<Long, Long> = attendees.associate { user ->
            user.id to (teamMemberRepository.findByTeamIdAndUserId(teamId, user.id)?.id ?: -1L)
        }
        val memberIds = memberByUser.values.filter { it != -1L }
        val statsByMember: Map<Long, List<PlateAppearance>> =
            if (memberIds.isEmpty()) emptyMap()
            else plateAppearanceRepository.findByTeamMemberIdIn(memberIds).groupBy { it.teamMember.id }

        val aiResponse = dugoutAiClient.recommendLineup(
            AiLineupRecommendRequest(
                matchId = matchId,
                attendees = attendees.map { user ->
                    val pas = statsByMember[memberByUser[user.id]] ?: emptyList()
                    fun n(r: BattingResult) = pas.count { it.result == r }
                    AiAttendeeProfile(
                        userId = user.id,
                        primaryPosition = "DH",
                        singles = n(BattingResult.SINGLE),
                        doubles = n(BattingResult.DOUBLE),
                        triples = n(BattingResult.TRIPLE),
                        homeRuns = n(BattingResult.HOME_RUN),
                        walks = n(BattingResult.WALK),
                        hitByPitch = n(BattingResult.HIT_BY_PITCH),
                        sacrificeFlies = n(BattingResult.SACRIFICE_FLY),
                        strikeouts = n(BattingResult.STRIKEOUT),
                        inPlayOuts = n(BattingResult.IN_PLAY_OUT),
                        reachedOnErrors = n(BattingResult.REACHED_ON_ERROR),
                    )
                },
                lineupMode = match.team.lineupMode.name,
            ),
        )
```

import 추가: `BattingResult`, `PlateAppearance`, `PlateAppearanceRepository`.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.dugout.api.domain.lineup.service.LineupServiceTest" --quiet`
Expected: PASS (기존 + 신규)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dugout/api/domain/lineup/service/LineupService.kt src/test/kotlin/com/dugout/api/domain/lineup/service/LineupServiceTest.kt
git commit -m "feat(lineup): 출석자 타석 기록을 집계해 AI 라인업 요청에 포함 (TDD)"
```

### Task C3: 전체 검증 + 문서 정합 확인

**Files:** (검증 전용, 코드 변경 없음)

- [ ] **Step 1: dugout-api 전체 테스트 + 린트**

Run: `./gradlew test ktlintCheck --quiet`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: dugout-ai 전체 테스트 + 린트 + 타입**

Run (in `dugout-ai/`): `pytest -q && ruff check . && mypy app/`
Expected: 전체 PASS

- [ ] **Step 3: 글로서리 위반 점검**

Run (저장소 루트): 글로서리 SKILL.md 7번 grep 3개 명령 실행.
Expected: 3개 모두 출력 없음(통과). 특히 `PlateAppearance`/`Record`/`OBP` 외 자유 번역·`AtBat`/`BattingStats` 미사용 확인.

- [ ] **Step 4: 문서 정합 확인**

`docs/TDD.md` 3-3-1·DB 스키마·API 표, `docs/PRD.md` F10이 구현과 일치하는지 확인(이미 갱신됨). 엔드포인트 경로(`/api/v1/records/plate-appearances`, `/teams/{teamId}/batting-stats`)가 RecordController와 일치하는지 대조. 불일치 시 TDD 표만 수정.

- [ ] **Step 5: 최종 커밋(문서 수정이 있었던 경우만)**

```bash
git add docs/
git commit -m "docs: 세이버매트릭스 타순 구현과 TDD/PRD 정합 반영"
```

---

## Self-Review 결과 (작성자 점검)

- **스펙 커버리지**: §2 데이터모델→A1/A3, §3 흐름·책임분리→C1/C2, §4-1 지표→B2, §4-2 shrinkage→B2, §4-3 슬로팅→B3, §4-4 콜드스타트→B3/B4, §4-5 모드(타순 공통)→B4(모드 무관 order 호출), §5 에러핸들링→A2/A5(ErrorCode·BusinessException), §6 테스트→B2/B3/B4 합성 픽스처, §7 문서→C3. 누락 없음.
- **스펙과의 의식적 차이**: ErrorCode를 `RECORD_MATCH_NOT_FOUND`/`RECORD_MEMBER_NOT_IN_TEAM`/`RECORD_INVALID_RESULT` 신설 대신 기존 `MATCH_NOT_FOUND`/`NOT_TEAM_MEMBER`/`INVALID_INPUT` 재사용(DRY) + `RECORD_NOT_FOUND`만 신설. soft delete→hard delete(BaseEntity에 deletedAt 없음). 두 변경은 스펙 의도를 해치지 않음.
- **타입 일관성**: Kotlin `AiAttendeeProfile` 카운트 필드명(camelCase) ↔ Python `AttendeeProfile`(snake_case)은 jackson naming strategy로 매핑. `order()` 반환 `dict[int,int]`(user_id→slot)을 hungarian이 `order_of[user_id]`로 사용 — 일치.
- **placeholder**: 없음. 단, A5/C2 테스트는 기존 테스트 파일의 셋업 상수·헬퍼에 맞춰 조정 필요(주석으로 명시). 구현 시 해당 파일을 먼저 읽을 것.
