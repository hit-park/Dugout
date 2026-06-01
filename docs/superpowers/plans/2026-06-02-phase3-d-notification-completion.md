# Phase 3-D Implementation Plan: 알림 완성 (Notification Completion)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phase 3-C(FCM 인프라 + 라인업 확정 broadcast)를 완성한다 — 푸시 탭 deeplink, 매치 등록/출석 변경 broadcast, 출석 리마인드 cron, 알림 설정(유형별 on/off + DnD).

**Architecture:** 백엔드는 기존 `LineupConfirmedEvent` + `@TransactionalEventListener(AFTER_COMMIT)` 패턴을 그대로 복제해 이벤트만 늘린다. 알림 종류는 `NotificationType` enum 하나로 통일해 (deeplink 키 / 설정 토글 키 / 발송 분기 키)를 일치시킨다. 리마인드는 `@Scheduled` 시간당 cron + per-user 멱등성 로그. iOS는 actor `PushPermissionCoordinator` 가 payload를 `@MainActor` `DeepLinkInbox` 에 적재하고 `MainTabView` 가 소비해 `AppRouter` 로 라우팅한다.

**Tech Stack:** Spring Boot 3.x + Kotlin, Firebase Admin SDK, Swift 6 strict concurrency + SwiftUI, Tuist 4.x.

**Reference Spec:** `docs/superpowers/specs/2026-06-02-phase3-d-notification-completion-design.md`

---

## 0. 사전 준비 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령
```bash
cd /Users/heetae/Documents/Source/Dugout
make api-test      # compileKotlin compileTestKotlin + 백엔드 테스트
make ios-build     # warnings 0
```

### 베이스 브랜치
이 plan은 spec이 이미 커밋된 `feature/phase3-d-notification-spec` 브랜치에서 이어간다. 모든 구현 커밋을 이 브랜치에 누적하고, 전체 완료 후 단일 머지로 main 반영.

```bash
git status                    # feature/phase3-d-notification-spec, clean
git log --oneline -1          # d587d06 docs(spec): Phase 3-D ...
```

### 기존 시그니처 (확인 완료 — 코드 블록은 이 사실에 맞춰 작성됨)
- `NotificationService` 생성자: `userRepository, matchRepository, teamMemberRepository, fcmClient, tokenCleanupService`
- `teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId): List<TeamMember>` / `TeamMember.role: TeamRole` / `TeamMember.user: User`
- `tokenCleanupService.clearInvalidTokens(invalidTokens: List<String>)`
- `match.team.id`, `match.matchDate: LocalDate`, `match.matchTime: LocalTime`, `match.voteDeadline: LocalDateTime?`, `match.status: MatchStatus`
- `MatchStatus`: SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
- `AttendanceStatus`: ATTEND, ABSENT, MAYBE, LATE, EARLY_LEAVE
- `TeamRole`: CAPTAIN, MANAGER, ACCOUNTANT, MEMBER
- `AttendanceService.updateVote`: `attendance.updateVote(status, reason)` 직전의 `attendance.status` 가 previous
- `MatchService.createMatch(userId, teamId, request)` → `Match.create(...)` save 후 `match.id` 확정
- DB 마이그레이션 마지막: `V1__drop_kakao_alimtalk_agreed.sql` → 다음은 V2, V3
- `@EnableScheduling` 미설정 (cron용 신규 필요)
- iOS `AppRouter`(@MainActor @Observable): `selectedTab: AppTab`, `schedulePath: NavigationPath`
- iOS `ScheduleTabHost`: `NavigationStack { MatchListView(...) }` — **path 바인딩 없음** (deeplink 위해 바인딩 추가 필요)
- iOS `PushPermissionCoordinator`: actor, `didReceive response` 비어 있음
- iOS `MainTabView(authViewModel:router:)`, `DugoutApp` 에서 `@State router/authViewModel`

### 전체 파일 목록

**신규 (백엔드):**
- `domain/notification/NotificationType.kt`
- `domain/notification/event/MatchCreatedEvent.kt`
- `domain/notification/event/AttendanceChangedEvent.kt`
- `domain/attendance/entity/AttendanceReminderLog.kt`
- `domain/attendance/entity/ReminderWindow.kt`
- `domain/attendance/repository/AttendanceReminderLogRepository.kt`
- `domain/notification/scheduler/AttendanceReminderScheduler.kt`
- `domain/notification/entity/NotificationPreference.kt`
- `domain/notification/repository/NotificationPreferenceRepository.kt`
- `domain/notification/service/NotificationPreferenceService.kt`
- `domain/notification/controller/NotificationPreferenceController.kt`
- `domain/notification/dto/NotificationPreferenceResponse.kt`
- `domain/notification/dto/NotificationPreferenceRequest.kt`
- `global/config/SchedulingConfig.kt`
- `resources/db/migration/V2__create_attendance_reminder_log.sql`
- `resources/db/migration/V3__create_notification_preference.sql`
- `src/test/kotlin/.../attendance/entity/AttendanceStatusTest.kt`
- `src/test/kotlin/.../notification/entity/NotificationPreferenceTest.kt`

**수정 (백엔드):**
- `domain/notification/service/NotificationService.kt` (NotificationType 사용 + 신규 listener 2 + preference gating)
- `domain/attendance/entity/AttendanceStatus.kt` (isAvailable + isMeaningfulChange)
- `domain/attendance/service/AttendanceService.kt` (이벤트 발행)
- `domain/attendance/repository/AttendanceRepository.kt` (findRespondedUserIds)
- `domain/match/service/MatchService.kt` (이벤트 발행)
- `domain/match/repository/MatchRepository.kt` (findByStatusAndMatchDateBetween)

**신규 (iOS):**
- `App/Sources/Notifications/NotificationType.swift`
- `App/Sources/Notifications/DeepLinkInbox.swift`
- `App/Sources/Notifications/NotificationPreferenceRepository.swift`
- `App/Sources/Notifications/NotificationSettingsViewModel.swift`
- `App/Sources/Notifications/NotificationSettingsView.swift`
- `Core/DesignSystem/Sources/Components/DGToggle.swift`

**수정 (iOS):**
- `App/Sources/Notifications/PushPermissionCoordinator.swift` (didReceive 파싱)
- `App/Sources/AppRouter.swift` (handlePush)
- `App/Sources/ScheduleTabHost.swift` (schedulePath 바인딩)
- `App/Sources/MainTabView.swift` (DeepLinkInbox 소비 + 설정 진입)

**수정 (문서):**
- `docs/TDD.md` (§4 이벤트 플로우 + 스케줄러 + 설정 + 2테이블 스키마)
- `docs/PRD.md` (F9 매트릭스 ✅)

---

## Milestone 1 (3-D-1) — NotificationType 표준화 + iOS deeplink

### Task 1.1: 백엔드 NotificationType enum + NotificationService 리팩터

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/NotificationType.kt`
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt`

- [ ] **Step 1: NotificationType 작성**

```kotlin
package com.dugout.api.domain.notification

enum class NotificationType {
    MATCH_CREATED,
    LINEUP_CONFIRMED,
    ATTENDANCE_REMINDER,
    ATTENDANCE_CHANGED,
}
```

- [ ] **Step 2: NotificationService 에 공통 data 헬퍼 추가 + 하드코딩 문자열 교체**

`NotificationService.kt` 의 `buildLineupConfirmedMessage` 안 `data = mapOf(...)` 를 헬퍼 호출로 교체하고, 클래스 하단에 헬퍼를 추가한다. import 추가: `import com.dugout.api.domain.notification.NotificationType`.

`buildLineupConfirmedMessage` 의 `return FcmMessage(...)` 를:

```kotlin
        return FcmMessage(
            title = "라인업이 확정됐어요",
            body = parts.joinToString(" · "),
            data = notificationData(
                type = NotificationType.LINEUP_CONFIRMED,
                matchId = match.id,
                teamId = match.team.id,
                lineupId = lineupId,
            ),
        )
```

클래스 맨 아래(`isValidFcmTokenShape` 아래)에 헬퍼 추가:

```kotlin
    private fun notificationData(
        type: NotificationType,
        matchId: Long,
        teamId: Long,
        lineupId: Long? = null,
    ): Map<String, String> = buildMap {
        put("type", type.name)
        put("matchId", matchId.toString())
        put("teamId", teamId.toString())
        lineupId?.let { put("lineupId", it.toString()) }
    }
```

- [ ] **Step 3: 빌드**

Run: `make api-test`
Expected: BUILD SUCCESSFUL. 기존 라인업 확정 payload의 `type` 값은 `"LINEUP_CONFIRMED"` 로 동일(회귀 없음).

- [ ] **Step 4: 하드코딩 잔존 확인**

Run: `grep -rn '"LINEUP_CONFIRMED"\|"MATCH_CREATED"\|"ATTENDANCE_' dugout-api/src/main/kotlin`
Expected: 0줄 (enum.name 으로만 생성).

### Task 1.2: iOS NotificationType + PushPayload

**Files:**
- Create: `dugout-ios/App/Sources/Notifications/NotificationType.swift`

- [ ] **Step 1: 작성** (rawValue가 백엔드 `.name` 과 1:1)

```swift
//
//  NotificationType.swift
//  Dugout
//

import Foundation

enum NotificationType: String, Sendable {
    case matchCreated = "MATCH_CREATED"
    case lineupConfirmed = "LINEUP_CONFIRMED"
    case attendanceReminder = "ATTENDANCE_REMINDER"
    case attendanceChanged = "ATTENDANCE_CHANGED"
}

struct PushRoute: Sendable, Equatable {
    let type: NotificationType
    let matchId: Int64?
}

extension PushRoute {
    /// APNs userInfo(`[AnyHashable: Any]`)에서 deeplink 라우트 추출. 실패 시 nil.
    init?(userInfo: [AnyHashable: Any]) {
        guard let rawType = userInfo["type"] as? String,
              let type = NotificationType(rawValue: rawType) else {
            return nil
        }
        let matchId = (userInfo["matchId"] as? String).flatMap { Int64($0) }
        self.init(type: type, matchId: matchId)
    }
}
```

- [ ] **Step 2: 빌드**

Run: `make ios-build`
Expected: BUILD SUCCEEDED, warnings 0.

### Task 1.3: iOS DeepLinkInbox + didReceive 파싱

**Files:**
- Create: `dugout-ios/App/Sources/Notifications/DeepLinkInbox.swift`
- Modify: `dugout-ios/App/Sources/Notifications/PushPermissionCoordinator.swift`

- [ ] **Step 1: DeepLinkInbox 작성**

```swift
//
//  DeepLinkInbox.swift
//  Dugout
//
//  푸시 탭 → 라우팅을 잇는 브리지. actor 코디네이터가 적재하고
//  MainTabView 가 소비한다. cold-start(앱 종료 중 푸시 탭)도 pending 보관으로 처리.
//

import Foundation

@MainActor
@Observable
final class DeepLinkInbox {
    static let shared = DeepLinkInbox()
    var pending: PushRoute?

    private init() {}

    func submit(_ route: PushRoute) {
        pending = route
    }

    func consume() -> PushRoute? {
        defer { pending = nil }
        return pending
    }
}
```

- [ ] **Step 2: PushPermissionCoordinator.didReceive 구현**

`PushPermissionCoordinator.swift` 의 비어 있는 `didReceive` 메서드 body를 교체:

```swift
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        guard let route = PushRoute(userInfo: userInfo) else { return }
        await DeepLinkInbox.shared.submit(route)
    }
```

- [ ] **Step 3: 빌드**

Run: `make ios-build`
Expected: BUILD SUCCEEDED, warnings 0.

### Task 1.4: AppRouter.handlePush + schedulePath 바인딩 + MainTabView 소비

**Files:**
- Modify: `dugout-ios/App/Sources/AppRouter.swift`
- Modify: `dugout-ios/App/Sources/ScheduleTabHost.swift`
- Modify: `dugout-ios/App/Sources/MainTabView.swift`

- [ ] **Step 1: AppRouter 에 handlePush 추가**

`AppRouter` 클래스 안 `navigate(to:)` 아래에 추가:

```swift
    public func handlePush(_ route: PushRoute) {
        selectedTab = .schedule
        guard let matchId = route.matchId else { return }
        schedulePath = NavigationPath()
        schedulePath.append(matchId)
    }
```

> `PushRoute` 는 App 타겟 타입이고 `AppRouter` 도 App 타겟이므로 import 불필요(동일 모듈).

- [ ] **Step 2: ScheduleTabHost 의 NavigationStack 을 router.schedulePath 에 바인딩**

`ScheduleTabHost` 에 `@Bindable var router: AppRouter` 프로퍼티를 추가하고(생성자 인자), `NavigationStack { ... }` 를 `NavigationStack(path: $router.schedulePath) { ... }` 로 교체.

프로퍼티 추가 (기존 `@Bindable var authViewModel` 아래):

```swift
    @Bindable var router: AppRouter
```

NavigationStack 교체:

```swift
                    NavigationStack(path: $router.schedulePath) {
                        MatchListView(
                            teamId: firstTeam.teamId,
                            isManager: firstTeam.role == .captain || firstTeam.role == .manager,
                            currentUserId: currentUserId
                        )
                    }
```

> `MatchListView` 가 `navigationDestination(for:)` 를 자체 등록하는지 확인: `grep -n "navigationDestination" dugout-ios/Features/Match/Sources/Presentation/Views/*.swift`. 등록 타입이 `Int64` 가 아니면(예: `Int` 또는 `match.id` 타입) `handlePush` 의 `append` 타입을 그 타입에 맞춘다. 현재 `match.id` 가 `Int64` 라는 전제. 불일치 시 이 Task에서 정렬.

- [ ] **Step 3: MainTabView 에서 router 전달 + DeepLinkInbox 소비**

`ScheduleTabHost(authViewModel:)` 호출에 router 추가:

```swift
        case .schedule:
            ScheduleTabHost(authViewModel: authViewModel, router: router)
```

`MainTabView` body의 최상위 `VStack { ... }` 에 inbox 관측 추가. 우선 프로퍼티 추가(기존 `@Bindable var router` 아래):

```swift
    private var inbox = DeepLinkInbox.shared
```

`VStack(spacing: 0) { ... }` 끝의 modifier 체인(`.background(DGColor.c100)` 아래)에 추가:

```swift
        .onChange(of: inbox.pending) { _, route in
            if let route { router.handlePush(route) ; inbox.pending = nil }
        }
        .task {
            if let route = inbox.consume() { router.handlePush(route) }
        }
```

> `.task` 는 cold-start(앱 진입 시 이미 pending 적재됨) 처리, `.onChange` 는 foreground 처리. `inbox` 를 `@Bindable`/`@State` 가 아닌 일반 프로퍼티로 두되 `@Observable` 이라 body에서 `inbox.pending` 접근이 관측을 트리거한다. 관측이 안 잡히면 `@State private var inbox = DeepLinkInbox.shared` 로 변경.

- [ ] **Step 4: 빌드**

Run: `make ios-build`
Expected: BUILD SUCCEEDED, warnings 0.

### Task 1.5: Milestone 1 커밋

- [ ] **Step 1: 백엔드 커밋**

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/notification/NotificationType.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt
git commit -m "feat(api): NotificationType enum 표준화 — payload type 하드코딩 제거 (Phase 3-D-1 1/2)"
```

- [ ] **Step 2: iOS 커밋**

```bash
git add dugout-ios/App/Sources/Notifications/NotificationType.swift \
        dugout-ios/App/Sources/Notifications/DeepLinkInbox.swift \
        dugout-ios/App/Sources/Notifications/PushPermissionCoordinator.swift \
        dugout-ios/App/Sources/AppRouter.swift \
        dugout-ios/App/Sources/ScheduleTabHost.swift \
        dugout-ios/App/Sources/MainTabView.swift
git commit -m "feat(ios): 푸시 탭 deeplink — DeepLinkInbox + schedulePath 라우팅 (Phase 3-D-1 2/2)"
```

---

## Milestone 2 (3-D-2) — 발송 확대 (MatchCreated / AttendanceChanged)

### Task 2.1: 의미있는 출석 변경 판정 (TDD)

**Files:**
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/entity/AttendanceStatus.kt`
- Test: `dugout-api/src/test/kotlin/com/dugout/api/domain/attendance/entity/AttendanceStatusTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.dugout.api.domain.attendance.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AttendanceStatusTest {
    @Test
    fun `참석 to 불참 은 의미있는 변경이다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ATTEND, AttendanceStatus.ABSENT)).isTrue()
    }

    @Test
    fun `불참 to 참석 은 의미있는 변경이다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ABSENT, AttendanceStatus.ATTEND)).isTrue()
    }

    @Test
    fun `참석 to 늦참 은 가용성 유지라 의미없다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ATTEND, AttendanceStatus.LATE)).isFalse()
    }

    @Test
    fun `미정 to 불참 은 둘 다 불가용이라 의미없다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.MAYBE, AttendanceStatus.ABSENT)).isFalse()
    }

    @Test
    fun `참석 to 미정 은 의미있는 변경이다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ATTEND, AttendanceStatus.MAYBE)).isTrue()
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-api && ./gradlew test --tests "com.dugout.api.domain.attendance.entity.AttendanceStatusTest" --quiet`
Expected: FAIL — `isMeaningfulAttendanceChange` unresolved reference.

- [ ] **Step 3: AttendanceStatus.kt 에 구현 추가**

기존 enum 선언 아래(파일 하단, enum 블록 밖)에 top-level 함수/확장 추가:

```kotlin
val AttendanceStatus.isAvailable: Boolean
    get() = this == AttendanceStatus.ATTEND ||
        this == AttendanceStatus.LATE ||
        this == AttendanceStatus.EARLY_LEAVE

fun isMeaningfulAttendanceChange(
    previous: AttendanceStatus,
    new: AttendanceStatus,
): Boolean = previous.isAvailable != new.isAvailable
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-api && ./gradlew test --tests "com.dugout.api.domain.attendance.entity.AttendanceStatusTest" --quiet`
Expected: PASS (5 tests).

- [ ] **Step 5: 커밋**

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/entity/AttendanceStatus.kt \
        dugout-api/src/test/kotlin/com/dugout/api/domain/attendance/entity/AttendanceStatusTest.kt
git commit -m "feat(api): 출석 가용성 경계 변경 판정 isMeaningfulAttendanceChange (Phase 3-D-2 1/4)"
```

### Task 2.2: 이벤트 클래스 2종

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/event/MatchCreatedEvent.kt`
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/event/AttendanceChangedEvent.kt`

- [ ] **Step 1: MatchCreatedEvent**

```kotlin
package com.dugout.api.domain.notification.event

data class MatchCreatedEvent(
    val matchId: Long,
    val teamId: Long,
    val createdBy: Long,
)
```

- [ ] **Step 2: AttendanceChangedEvent**

```kotlin
package com.dugout.api.domain.notification.event

import com.dugout.api.domain.attendance.entity.AttendanceStatus

data class AttendanceChangedEvent(
    val matchId: Long,
    val teamId: Long,
    val actorUserId: Long,
    val previous: AttendanceStatus,
    val new: AttendanceStatus,
)
```

- [ ] **Step 3: 빌드**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

### Task 2.3: MatchService / AttendanceService 이벤트 발행

**Files:**
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/match/service/MatchService.kt`
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/service/AttendanceService.kt`

- [ ] **Step 1: MatchService 에 publisher 주입 + 발행**

생성자에 `private val eventPublisher: org.springframework.context.ApplicationEventPublisher` 추가. import:

```kotlin
import com.dugout.api.domain.notification.event.MatchCreatedEvent
import org.springframework.context.ApplicationEventPublisher
```

`createMatch` 의 `return MatchResponse.from(match)` 직전에 추가:

```kotlin
        eventPublisher.publishEvent(
            MatchCreatedEvent(matchId = match.id, teamId = match.team.id, createdBy = userId),
        )
```

- [ ] **Step 2: AttendanceService 에 publisher 주입 + 발행**

생성자에 `private val eventPublisher: ApplicationEventPublisher` 추가. import:

```kotlin
import com.dugout.api.domain.notification.event.AttendanceChangedEvent
import org.springframework.context.ApplicationEventPublisher
```

`updateVote` 에서 `attendance.updateVote(...)` 호출을 previous 캡처로 감싸고 발행 추가. 기존:

```kotlin
        val attendance = attendanceRepository.findByMatchIdAndUserId(matchId, userId)
            ?: throw BusinessException(ErrorCode.VOTE_NOT_FOUND)

        attendance.updateVote(status, request.reason)
        return AttendanceResponse.from(attendance)
```

교체:

```kotlin
        val attendance = attendanceRepository.findByMatchIdAndUserId(matchId, userId)
            ?: throw BusinessException(ErrorCode.VOTE_NOT_FOUND)

        val previous = attendance.status
        attendance.updateVote(status, request.reason)

        eventPublisher.publishEvent(
            AttendanceChangedEvent(
                matchId = matchId,
                teamId = match.team.id,
                actorUserId = userId,
                previous = previous,
                new = status,
            ),
        )
        return AttendanceResponse.from(attendance)
```

- [ ] **Step 3: 빌드**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

### Task 2.4: NotificationService 리스너 2종

**Files:**
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt`

- [ ] **Step 1: import 추가**

```kotlin
import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.entity.isMeaningfulAttendanceChange
import com.dugout.api.domain.notification.event.AttendanceChangedEvent
import com.dugout.api.domain.notification.event.MatchCreatedEvent
import com.dugout.api.domain.team.entity.TeamRole
```

- [ ] **Step 2: onMatchCreated 리스너 추가** (클래스 안 `onLineupConfirmed` 아래)

```kotlin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMatchCreated(event: MatchCreatedEvent) {
        val members = teamMemberRepository.findByTeamIdAndIsActiveTrue(event.teamId)
        val targetUsers = members.map { it.user }.filter { it.id != event.createdBy }
        val tokens = targetUsers.mapNotNull { it.fcmToken }
        if (tokens.isEmpty()) return

        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = FcmMessage(
            title = "새 경기 일정이 등록됐어요",
            body = matchSummary(match),
            data = notificationData(NotificationType.MATCH_CREATED, match.id, match.team.id),
        )
        val result = fcmClient.sendToTokens(tokens, payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)
    }
```

- [ ] **Step 3: onAttendanceChanged 리스너 추가**

```kotlin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAttendanceChanged(event: AttendanceChangedEvent) {
        if (!isMeaningfulAttendanceChange(event.previous, event.new)) return

        val captain = teamMemberRepository.findByTeamIdAndIsActiveTrue(event.teamId)
            .firstOrNull { it.role == TeamRole.CAPTAIN } ?: return
        if (captain.user.id == event.actorUserId) return
        val token = captain.user.fcmToken ?: return

        val actor = userRepository.findById(event.actorUserId).orElse(null) ?: return
        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = FcmMessage(
            title = "출석 응답이 변경됐어요",
            body = "${actor.nickname}님 · ${attendanceLabel(event.new)} · ${matchSummary(match)}",
            data = notificationData(NotificationType.ATTENDANCE_CHANGED, match.id, match.team.id),
        )
        val result = fcmClient.sendToTokens(listOf(token), payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)
    }
```

- [ ] **Step 4: 공통 헬퍼 2개 추가** (클래스 하단 `notificationData` 아래)

```kotlin
    private fun matchSummary(match: Match): String {
        val date = match.matchDate
        val dateText = "${date.monthValue}월 ${date.dayOfMonth}일 (${
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        })"
        return buildList {
            add(dateText)
            match.groundName?.let { add(it) }
            match.opponentName?.let { add("vs $it") }
        }.joinToString(" · ")
    }

    private fun attendanceLabel(status: AttendanceStatus): String = when (status) {
        AttendanceStatus.ATTEND -> "참석"
        AttendanceStatus.ABSENT -> "불참"
        AttendanceStatus.MAYBE -> "미정"
        AttendanceStatus.LATE -> "늦참"
        AttendanceStatus.EARLY_LEAVE -> "조퇴"
    }
```

> `buildLineupConfirmedMessage` 의 dateText 생성 부분도 `matchSummary` 로 중복 제거 가능하나, 회귀 위험 최소화를 위해 이 Task에서는 신규 헬퍼만 추가하고 기존은 유지(후속 정리).

- [ ] **Step 5: 빌드 + 커밋**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/notification/event/ \
        dugout-api/src/main/kotlin/com/dugout/api/domain/match/service/MatchService.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/service/AttendanceService.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt
git commit -m "feat(api): 매치 등록/출석 변경 broadcast — Event + AFTER_COMMIT Listener (Phase 3-D-2 4/4)"
```

---

## Milestone 3 (3-D-3) — 출석 리마인드 cron

### Task 3.1: ReminderWindow + AttendanceReminderLog 엔티티 + 마이그레이션

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/entity/ReminderWindow.kt`
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/entity/AttendanceReminderLog.kt`
- Create: `dugout-api/src/main/resources/db/migration/V2__create_attendance_reminder_log.sql`

- [ ] **Step 1: ReminderWindow**

```kotlin
package com.dugout.api.domain.attendance.entity

enum class ReminderWindow(val hoursBefore: Long) {
    H48(48),
    H24(24),
}
```

- [ ] **Step 2: AttendanceReminderLog**

```kotlin
package com.dugout.api.domain.attendance.entity

import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "attendance_reminder_logs",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["match_id", "user_id", "reminder_window"]),
    ],
)
class AttendanceReminderLog(
    @Column(name = "match_id", nullable = false)
    val matchId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_window", nullable = false, length = 10)
    val reminderWindow: ReminderWindow,
) : BaseEntity()
```

- [ ] **Step 3: 마이그레이션**

```sql
CREATE TABLE attendance_reminder_logs (
    id              BIGSERIAL PRIMARY KEY,
    match_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    reminder_window VARCHAR(10) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP,
    CONSTRAINT uq_reminder UNIQUE (match_id, user_id, reminder_window)
);
```

> `created_at/updated_at/deleted_at` 컬럼은 `BaseEntity` 매핑 규약에 맞춘다. 기존 엔티티 테이블의 BaseEntity 컬럼 정의를 `grep -n "created_at\|deleted_at" dugout-api/src/main/resources/db/migration/*.sql` 또는 기존 엔티티의 @Column 으로 확인 후 정렬(soft delete 사용 여부 포함).

- [ ] **Step 4: 빌드**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

### Task 3.2: Repository 메서드 신규

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/repository/AttendanceReminderLogRepository.kt`
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/repository/AttendanceRepository.kt`
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/match/repository/MatchRepository.kt`

- [ ] **Step 1: AttendanceReminderLogRepository**

```kotlin
package com.dugout.api.domain.attendance.repository

import com.dugout.api.domain.attendance.entity.AttendanceReminderLog
import com.dugout.api.domain.attendance.entity.ReminderWindow
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceReminderLogRepository : JpaRepository<AttendanceReminderLog, Long> {
    fun existsByMatchIdAndUserIdAndReminderWindow(
        matchId: Long,
        userId: Long,
        reminderWindow: ReminderWindow,
    ): Boolean
}
```

- [ ] **Step 2: AttendanceRepository 에 응답자 id 조회 추가**

```kotlin
import org.springframework.data.jpa.repository.Query
```
인터페이스 안에 추가:

```kotlin
    @Query("SELECT a.user.id FROM Attendance a WHERE a.match.id = :matchId")
    fun findRespondedUserIds(matchId: Long): List<Long>
```

- [ ] **Step 3: MatchRepository 에 status+날짜범위 조회 추가**

```kotlin
import com.dugout.api.domain.match.entity.MatchStatus
```
인터페이스 안에 추가:

```kotlin
    fun findByStatusAndMatchDateBetween(
        status: MatchStatus,
        from: LocalDate,
        to: LocalDate,
    ): List<Match>
```

- [ ] **Step 4: 빌드**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

### Task 3.3: @EnableScheduling 설정

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/global/config/SchedulingConfig.kt`

- [ ] **Step 1: 작성**

```kotlin
package com.dugout.api.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class SchedulingConfig
```

- [ ] **Step 2: 빌드**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

### Task 3.4: AttendanceReminderScheduler

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/scheduler/AttendanceReminderScheduler.kt`

- [ ] **Step 1: 작성**

```kotlin
package com.dugout.api.domain.notification.scheduler

import com.dugout.api.domain.attendance.entity.AttendanceReminderLog
import com.dugout.api.domain.attendance.entity.ReminderWindow
import com.dugout.api.domain.attendance.repository.AttendanceReminderLogRepository
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.entity.MatchStatus
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.notification.NotificationType
import com.dugout.api.domain.notification.service.TokenCleanupService
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.fcm.FcmClient
import com.dugout.api.global.fcm.FcmMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

@Component
class AttendanceReminderScheduler(
    private val matchRepository: MatchRepository,
    private val attendanceRepository: AttendanceRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val reminderLogRepository: AttendanceReminderLogRepository,
    private val fcmClient: FcmClient,
    private val tokenCleanupService: TokenCleanupService,
) {
    private val log = LoggerFactory.getLogger(AttendanceReminderScheduler::class.java)

    @Scheduled(cron = "0 0 * * * *")  // 매시 정각
    @Transactional
    fun sendReminders() {
        val now = LocalDateTime.now()
        ReminderWindow.entries.forEach { window -> sendForWindow(window, now) }
    }

    private fun sendForWindow(window: ReminderWindow, now: LocalDateTime) {
        val bucketStart = now.plusHours(window.hoursBefore)
        val bucketEnd = bucketStart.plusHours(1)
        val candidates = matchRepository.findByStatusAndMatchDateBetween(
            MatchStatus.SCHEDULED,
            bucketStart.toLocalDate(),
            bucketEnd.toLocalDate(),
        )
        candidates.forEach { match ->
            val start = LocalDateTime.of(match.matchDate, match.matchTime)
            if (start < bucketStart || start >= bucketEnd) return@forEach
            val deadline = match.voteDeadline
            if (deadline != null && now.isAfter(deadline)) return@forEach
            remindNonResponders(match, window)
        }
    }

    private fun remindNonResponders(match: Match, window: ReminderWindow) {
        val respondedIds = attendanceRepository.findRespondedUserIds(match.id).toSet()
        val nonResponders = teamMemberRepository.findByTeamIdAndIsActiveTrue(match.team.id)
            .map { it.user }
            .filter { it.id !in respondedIds }
            .filter { !reminderLogRepository.existsByMatchIdAndUserIdAndReminderWindow(match.id, it.id, window) }

        val targets = nonResponders.filter { it.fcmToken != null }
        if (targets.isEmpty()) return

        val payload = FcmMessage(
            title = "출석 응답을 잊지 않으셨나요?",
            body = matchSummary(match),
            data = mapOf(
                "type" to NotificationType.ATTENDANCE_REMINDER.name,
                "matchId" to match.id.toString(),
                "teamId" to match.team.id.toString(),
            ),
        )
        val tokens = targets.mapNotNull { it.fcmToken }
        val result = fcmClient.sendToTokens(tokens, payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)

        targets
            .filter { it.fcmToken !in result.invalidTokens }
            .forEach { user -> markSent(match.id, user.id, window) }
        log.info("reminder sent: match=${match.id} window=$window targets=${targets.size}")
    }

    private fun markSent(matchId: Long, userId: Long, window: ReminderWindow) {
        reminderLogRepository.save(AttendanceReminderLog(matchId, userId, window))
    }

    private fun matchSummary(match: Match): String {
        val date = match.matchDate
        val dateText = "${date.monthValue}월 ${date.dayOfMonth}일 (${
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        })"
        return buildList {
            add(dateText)
            match.groundName?.let { add(it) }
            match.opponentName?.let { add("vs $it") }
        }.joinToString(" · ")
    }
}
```

> `User` import는 `nonResponders` 타입 추론용. 미사용 경고 시 제거. M4에서 이 클래스에 preference/DnD 필터를 `targets` 산출 단계에 끼워 넣는다.

- [ ] **Step 2: 빌드 + 커밋**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/entity/ReminderWindow.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/entity/AttendanceReminderLog.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/attendance/repository/ \
        dugout-api/src/main/kotlin/com/dugout/api/domain/match/repository/MatchRepository.kt \
        dugout-api/src/main/kotlin/com/dugout/api/global/config/SchedulingConfig.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/scheduler/ \
        dugout-api/src/main/resources/db/migration/V2__create_attendance_reminder_log.sql
git commit -m "feat(api): 출석 리마인드 cron — 48h/24h, per-user 멱등성 로그 (Phase 3-D-3 1/1)"
```

---

## Milestone 4 (3-D-4) — 알림 설정 (NotificationPreference + DnD) + iOS

### Task 4.1: DnD 시간 판정 (TDD) + NotificationPreference 엔티티 + 마이그레이션

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/entity/NotificationPreference.kt`
- Create: `dugout-api/src/main/resources/db/migration/V3__create_notification_preference.sql`
- Test: `dugout-api/src/test/kotlin/com/dugout/api/domain/notification/entity/NotificationPreferenceTest.kt`

- [ ] **Step 1: 실패하는 DnD 테스트 작성**

```kotlin
package com.dugout.api.domain.notification.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalTime

class NotificationPreferenceTest {
    private fun pref() = NotificationPreference(
        userId = 1L,
        dndEnabled = true,
        dndStart = LocalTime.of(22, 0),
        dndEnd = LocalTime.of(8, 0),
    )

    @Test
    fun `자정 넘김 23시는 DnD 구간이다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(23, 0))).isTrue()
    }

    @Test
    fun `새벽 2시는 DnD 구간이다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(2, 0))).isTrue()
    }

    @Test
    fun `오전 9시는 DnD 구간이 아니다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(9, 0))).isFalse()
    }

    @Test
    fun `종료시각 8시 정각은 DnD 밖이다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(8, 0))).isFalse()
    }

    @Test
    fun `dnd 비활성화면 항상 false`() {
        val p = NotificationPreference(userId = 1L, dndEnabled = false,
            dndStart = LocalTime.of(22, 0), dndEnd = LocalTime.of(8, 0))
        assertThat(p.isWithinDnd(LocalTime.of(23, 0))).isFalse()
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-api && ./gradlew test --tests "com.dugout.api.domain.notification.entity.NotificationPreferenceTest" --quiet`
Expected: FAIL — unresolved reference NotificationPreference.

- [ ] **Step 3: NotificationPreference 엔티티 작성**

```kotlin
package com.dugout.api.domain.notification.entity

import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "notification_preferences")
class NotificationPreference(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Column(name = "match_created", nullable = false)
    var matchCreated: Boolean = true,

    @Column(name = "lineup_confirmed", nullable = false)
    var lineupConfirmed: Boolean = true,

    @Column(name = "attendance_reminder", nullable = false)
    var attendanceReminder: Boolean = true,

    @Column(name = "attendance_changed", nullable = false)
    var attendanceChanged: Boolean = true,

    @Column(name = "dnd_enabled", nullable = false)
    var dndEnabled: Boolean = true,

    @Column(name = "dnd_start", nullable = false)
    var dndStart: LocalTime = LocalTime.of(22, 0),

    @Column(name = "dnd_end", nullable = false)
    var dndEnd: LocalTime = LocalTime.of(8, 0),
) : BaseEntity() {

    fun isWithinDnd(now: LocalTime): Boolean {
        if (!dndEnabled) return false
        return if (dndStart <= dndEnd) {
            now >= dndStart && now < dndEnd
        } else {
            now >= dndStart || now < dndEnd
        }
    }

    fun isEnabled(type: com.dugout.api.domain.notification.NotificationType): Boolean = when (type) {
        com.dugout.api.domain.notification.NotificationType.MATCH_CREATED -> matchCreated
        com.dugout.api.domain.notification.NotificationType.LINEUP_CONFIRMED -> lineupConfirmed
        com.dugout.api.domain.notification.NotificationType.ATTENDANCE_REMINDER -> attendanceReminder
        com.dugout.api.domain.notification.NotificationType.ATTENDANCE_CHANGED -> attendanceChanged
    }

    companion object {
        fun default(userId: Long) = NotificationPreference(userId = userId)
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-api && ./gradlew test --tests "com.dugout.api.domain.notification.entity.NotificationPreferenceTest" --quiet`
Expected: PASS (5 tests).

- [ ] **Step 5: 마이그레이션 작성**

```sql
CREATE TABLE notification_preferences (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    match_created       BOOLEAN NOT NULL DEFAULT true,
    lineup_confirmed    BOOLEAN NOT NULL DEFAULT true,
    attendance_reminder BOOLEAN NOT NULL DEFAULT true,
    attendance_changed  BOOLEAN NOT NULL DEFAULT true,
    dnd_enabled         BOOLEAN NOT NULL DEFAULT true,
    dnd_start           TIME NOT NULL DEFAULT '22:00',
    dnd_end             TIME NOT NULL DEFAULT '08:00',
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP
);
```

> Task 3.1과 동일하게 BaseEntity 컬럼 규약(soft delete 여부) 확인 후 정렬.

- [ ] **Step 6: 커밋**

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/notification/entity/NotificationPreference.kt \
        dugout-api/src/main/resources/db/migration/V3__create_notification_preference.sql \
        dugout-api/src/test/kotlin/com/dugout/api/domain/notification/entity/NotificationPreferenceTest.kt
git commit -m "feat(api): NotificationPreference 엔티티 + DnD 시간 판정 (Phase 3-D-4 1/4)"
```

### Task 4.2: Repository + Service + DTO + Controller

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/repository/NotificationPreferenceRepository.kt`
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/NotificationPreferenceResponse.kt`
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/NotificationPreferenceRequest.kt`
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationPreferenceService.kt`
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/controller/NotificationPreferenceController.kt`

- [ ] **Step 1: Repository**

```kotlin
package com.dugout.api.domain.notification.repository

import com.dugout.api.domain.notification.entity.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findByUserId(userId: Long): NotificationPreference?
    fun findByUserIdIn(userIds: Collection<Long>): List<NotificationPreference>
}
```

- [ ] **Step 2: DTO 2종**

`NotificationPreferenceResponse.kt`:

```kotlin
package com.dugout.api.domain.notification.dto

import com.dugout.api.domain.notification.entity.NotificationPreference
import java.time.LocalTime

data class NotificationPreferenceResponse(
    val matchCreated: Boolean,
    val lineupConfirmed: Boolean,
    val attendanceReminder: Boolean,
    val attendanceChanged: Boolean,
    val dndEnabled: Boolean,
    val dndStart: LocalTime,
    val dndEnd: LocalTime,
) {
    companion object {
        fun from(p: NotificationPreference) = NotificationPreferenceResponse(
            matchCreated = p.matchCreated,
            lineupConfirmed = p.lineupConfirmed,
            attendanceReminder = p.attendanceReminder,
            attendanceChanged = p.attendanceChanged,
            dndEnabled = p.dndEnabled,
            dndStart = p.dndStart,
            dndEnd = p.dndEnd,
        )
    }
}
```

`NotificationPreferenceRequest.kt`:

```kotlin
package com.dugout.api.domain.notification.dto

import java.time.LocalTime

data class NotificationPreferenceRequest(
    val matchCreated: Boolean,
    val lineupConfirmed: Boolean,
    val attendanceReminder: Boolean,
    val attendanceChanged: Boolean,
    val dndEnabled: Boolean,
    val dndStart: LocalTime,
    val dndEnd: LocalTime,
)
```

- [ ] **Step 3: Service** (없으면 기본값 lazy 생성)

```kotlin
package com.dugout.api.domain.notification.service

import com.dugout.api.domain.notification.dto.NotificationPreferenceRequest
import com.dugout.api.domain.notification.dto.NotificationPreferenceResponse
import com.dugout.api.domain.notification.entity.NotificationPreference
import com.dugout.api.domain.notification.repository.NotificationPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationPreferenceService(
    private val repository: NotificationPreferenceRepository,
) {
    @Transactional
    fun get(userId: Long): NotificationPreferenceResponse {
        val pref = repository.findByUserId(userId)
            ?: repository.save(NotificationPreference.default(userId))
        return NotificationPreferenceResponse.from(pref)
    }

    @Transactional
    fun update(userId: Long, request: NotificationPreferenceRequest): NotificationPreferenceResponse {
        val pref = repository.findByUserId(userId)
            ?: NotificationPreference.default(userId)
        pref.matchCreated = request.matchCreated
        pref.lineupConfirmed = request.lineupConfirmed
        pref.attendanceReminder = request.attendanceReminder
        pref.attendanceChanged = request.attendanceChanged
        pref.dndEnabled = request.dndEnabled
        pref.dndStart = request.dndStart
        pref.dndEnd = request.dndEnd
        return NotificationPreferenceResponse.from(repository.save(pref))
    }
}
```

- [ ] **Step 4: Controller** (기존 `NotificationController` 의 `@LoginUser` 패턴 따름 — 먼저 확인)

Run: `grep -n "LoginUser\|@RequestMapping\|@PatchMapping" dugout-api/src/main/kotlin/com/dugout/api/domain/notification/controller/NotificationController.kt`
확인된 인증 인자 패턴을 그대로 사용.

```kotlin
package com.dugout.api.domain.notification.controller

import com.dugout.api.domain.notification.dto.NotificationPreferenceRequest
import com.dugout.api.domain.notification.dto.NotificationPreferenceResponse
import com.dugout.api.domain.notification.service.NotificationPreferenceService
import com.dugout.api.global.auth.LoginUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
class NotificationPreferenceController(
    private val service: NotificationPreferenceService,
) {
    @GetMapping
    fun get(@LoginUser userId: Long): ResponseEntity<NotificationPreferenceResponse> =
        ResponseEntity.ok(service.get(userId))

    @PutMapping
    fun update(
        @LoginUser userId: Long,
        @RequestBody request: NotificationPreferenceRequest,
    ): ResponseEntity<NotificationPreferenceResponse> =
        ResponseEntity.ok(service.update(userId, request))
}
```

> `@LoginUser` 가 실제 어노테이션명과 다르면 Step 4 grep 결과로 정렬.

- [ ] **Step 5: 빌드 + 커밋**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/notification/repository/NotificationPreferenceRepository.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/NotificationPreference*.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationPreferenceService.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/controller/NotificationPreferenceController.kt
git commit -m "feat(api): 알림 설정 GET/PUT API + lazy 기본값 생성 (Phase 3-D-4 2/4)"
```

### Task 4.3: 발송 경로 preference gating

**Files:**
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt`
- Modify: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/scheduler/AttendanceReminderScheduler.kt`

- [ ] **Step 1: NotificationService 에 preference repo 주입 + 필터 헬퍼**

생성자에 추가: `private val preferenceRepository: com.dugout.api.domain.notification.repository.NotificationPreferenceRepository`.

클래스 하단에 헬퍼 추가:

```kotlin
    private fun filterByPreference(
        users: List<User>,
        type: NotificationType,
    ): List<User> {
        if (users.isEmpty()) return users
        val prefs = preferenceRepository.findByUserIdIn(users.mapNotNull { it.id })
            .associateBy { it.userId }
        return users.filter { user ->
            // row 없으면 기본 on
            prefs[user.id]?.isEnabled(type) ?: true
        }
    }
```

import 추가: `import com.dugout.api.domain.user.entity.User` (이미 있으면 skip).

- [ ] **Step 2: 세 listener 의 대상 산출에 필터 적용**

`onLineupConfirmed`: `val targetUsers = members.map { it.user }.filter { it.id != event.confirmedBy }` 다음 줄을 `val filtered = filterByPreference(targetUsers, NotificationType.LINEUP_CONFIRMED)` 로 바꾸고 이후 `targetUsers` → `filtered` 사용.

`onMatchCreated`: 동일하게 `filterByPreference(targetUsers, NotificationType.MATCH_CREATED)`.

`onAttendanceChanged`: 주장 단일 대상 → 발송 직전에 가드 추가:

```kotlin
        if (filterByPreference(listOf(captain.user), NotificationType.ATTENDANCE_CHANGED).isEmpty()) return
```

- [ ] **Step 3: 스케줄러에 preference + DnD 필터 적용**

`AttendanceReminderScheduler` 생성자에 추가: `private val preferenceRepository: NotificationPreferenceRepository`.
import: `import com.dugout.api.domain.notification.repository.NotificationPreferenceRepository`, `import java.time.LocalTime`.

`remindNonResponders` 의 `val targets = nonResponders.filter { it.fcmToken != null }` 를 교체:

```kotlin
        val nowTime = LocalTime.now()
        val prefs = preferenceRepository.findByUserIdIn(nonResponders.mapNotNull { it.id })
            .associateBy { it.userId }
        val targets = nonResponders.filter { user ->
            if (user.fcmToken == null) return@filter false
            val pref = prefs[user.id]
            if (pref != null && !pref.attendanceReminder) return@filter false
            // DnD 구간이면 이번엔 skip → 로그 미기록 → 다음 정시(아침)에 재시도
            if (pref != null && pref.isWithinDnd(nowTime)) return@filter false
            true
        }
```

> DnD로 skip된 미응답자는 로그가 남지 않으므로 DnD 종료 후 첫 정시 실행에서 발송된다(연기). 즉시성 알림(M2 listener)은 DnD를 적용하지 않는다(필터 헬퍼에 DnD 분기 없음).

- [ ] **Step 4: 빌드 + 커밋**

Run: `make api-test`
Expected: BUILD SUCCESSFUL.

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/scheduler/AttendanceReminderScheduler.kt
git commit -m "feat(api): 발송 경로 preference gating + 리마인드 DnD 연기 (Phase 3-D-4 3/4)"
```

### Task 4.4: iOS DGToggle 컴포넌트

**Files:**
- Create: `dugout-ios/Core/DesignSystem/Sources/Components/DGToggle.swift`

- [ ] **Step 1: 작성** (DesignSystem 내부는 raw SwiftUI 허용)

```swift
//
//  DGToggle.swift
//  DugoutDesignSystem
//

import SwiftUI

public struct DGToggle: View {
    private let title: String
    @Binding private var isOn: Bool

    public init(_ title: String, isOn: Binding<Bool>) {
        self.title = title
        self._isOn = isOn
    }

    public var body: some View {
        Toggle(isOn: $isOn) {
            Text(title)
                .font(DGFont.pretendard(.regular, size: 16))
                .foregroundStyle(DGColor.c900)
        }
        .tint(DGColor.p500)
    }
}
```

> `DGFont.pretendard` / `DGColor.p500` / `DGColor.c900` 토큰명은 기존 컴포넌트(`DGButton.swift` 등) 사용형과 일치 확인 후 정렬.

- [ ] **Step 2: 빌드**

Run: `make ios-build`
Expected: BUILD SUCCEEDED, warnings 0.

### Task 4.5: iOS 알림 설정 Repository + ViewModel + View + 진입

**Files:**
- Create: `dugout-ios/App/Sources/Notifications/NotificationPreferenceRepository.swift`
- Create: `dugout-ios/App/Sources/Notifications/NotificationSettingsViewModel.swift`
- Create: `dugout-ios/App/Sources/Notifications/NotificationSettingsView.swift`
- Modify: `dugout-ios/App/Sources/MainTabView.swift` (또는 MyPage 진입점)

- [ ] **Step 1: Repository** (3-C `NotificationRepository.swift` 의 APIClient/APIEndpoint 패턴 따름)

먼저 확인: `cat dugout-ios/App/Sources/Notifications/NotificationRepository.swift` — `APIClient`/`APIEndpoint`/`request` 시그니처를 그대로 사용.

```swift
//
//  NotificationPreferenceRepository.swift
//  Dugout
//

import Foundation
import DugoutCoreNetwork

struct NotificationPreferenceDTO: Codable, Sendable {
    var matchCreated: Bool
    var lineupConfirmed: Bool
    var attendanceReminder: Bool
    var attendanceChanged: Bool
    var dndEnabled: Bool
    var dndStart: String   // "HH:mm:ss"
    var dndEnd: String
}

protocol NotificationPreferenceRepository: Sendable {
    func fetch() async throws -> NotificationPreferenceDTO
    func update(_ dto: NotificationPreferenceDTO) async throws -> NotificationPreferenceDTO
}

final class NotificationPreferenceRepositoryImpl: NotificationPreferenceRepository {
    private let client: APIClient
    init(client: APIClient = .shared) { self.client = client }

    func fetch() async throws -> NotificationPreferenceDTO {
        let endpoint = APIEndpoint(path: "/api/v1/users/me/notification-preferences", method: .get)
        return try await client.request(endpoint)
    }

    func update(_ dto: NotificationPreferenceDTO) async throws -> NotificationPreferenceDTO {
        let endpoint = APIEndpoint(
            path: "/api/v1/users/me/notification-preferences",
            method: .put,
            body: dto
        )
        return try await client.request(endpoint)
    }
}
```

> `APIEndpoint` 의 `body`/`method` enum/생성자 형태가 다르면 `NotificationRepository.swift` 의 실제 사용형에 맞춰 정렬. `dndStart` 직렬화 포맷(백엔드 `LocalTime` 기본 "HH:mm:ss")은 `make api-test` 후 통합 검증에서 확인.

- [ ] **Step 2: ViewModel**

```swift
//
//  NotificationSettingsViewModel.swift
//  Dugout
//

import Foundation

@MainActor
final class NotificationSettingsViewModel: ObservableObject {
    @Published var matchCreated = true
    @Published var lineupConfirmed = true
    @Published var attendanceReminder = true
    @Published var attendanceChanged = true
    @Published var dndEnabled = true
    @Published var dndStart = Self.time(22, 0)
    @Published var dndEnd = Self.time(8, 0)
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let repository: any NotificationPreferenceRepository

    init(repository: any NotificationPreferenceRepository = NotificationPreferenceRepositoryImpl()) {
        self.repository = repository
    }

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            apply(try await repository.fetch())
        } catch {
            errorMessage = "알림 설정을 불러오지 못했습니다"
        }
    }

    func save() async {
        do {
            apply(try await repository.update(currentDTO()))
        } catch {
            errorMessage = "알림 설정 저장에 실패했습니다"
        }
    }

    private func apply(_ dto: NotificationPreferenceDTO) {
        matchCreated = dto.matchCreated
        lineupConfirmed = dto.lineupConfirmed
        attendanceReminder = dto.attendanceReminder
        attendanceChanged = dto.attendanceChanged
        dndEnabled = dto.dndEnabled
        dndStart = Self.parse(dto.dndStart) ?? dndStart
        dndEnd = Self.parse(dto.dndEnd) ?? dndEnd
    }

    private func currentDTO() -> NotificationPreferenceDTO {
        NotificationPreferenceDTO(
            matchCreated: matchCreated,
            lineupConfirmed: lineupConfirmed,
            attendanceReminder: attendanceReminder,
            attendanceChanged: attendanceChanged,
            dndEnabled: dndEnabled,
            dndStart: Self.format(dndStart),
            dndEnd: Self.format(dndEnd)
        )
    }

    private static func time(_ h: Int, _ m: Int) -> Date {
        Calendar.current.date(from: DateComponents(hour: h, minute: m)) ?? Date()
    }
    private static func parse(_ s: String) -> Date? {
        let f = DateFormatter(); f.dateFormat = "HH:mm:ss"; return f.date(from: s)
    }
    private static func format(_ d: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "HH:mm:ss"; return f.string(from: d)
    }
}
```

- [ ] **Step 3: View**

```swift
//
//  NotificationSettingsView.swift
//  Dugout
//

import SwiftUI
import DugoutDesignSystem

struct NotificationSettingsView: View {
    @StateObject private var viewModel = NotificationSettingsViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DGSpacing.lg) {
                DGToggle("새 경기 일정", isOn: $viewModel.matchCreated)
                DGToggle("라인업 확정", isOn: $viewModel.lineupConfirmed)
                DGToggle("출석 리마인드", isOn: $viewModel.attendanceReminder)
                DGToggle("출석 응답 변경(주장)", isOn: $viewModel.attendanceChanged)

                Divider()

                DGToggle("방해 금지 시간", isOn: $viewModel.dndEnabled)
                if viewModel.dndEnabled {
                    DatePicker("시작", selection: $viewModel.dndStart, displayedComponents: .hourAndMinute)
                    DatePicker("종료", selection: $viewModel.dndEnd, displayedComponents: .hourAndMinute)
                }
            }
            .padding(DGSpacing.lg)
        }
        .navigationTitle("알림 설정")
        .background(DGColor.c100)
        .task { await viewModel.load() }
        .onDisappear { Task { await viewModel.save() } }
    }
}
```

> `DGSpacing.lg` 등 토큰명은 기존 View 사용형 확인 후 정렬. `DatePicker` 는 설정 화면 한정 raw 허용(또는 DesignSystem에 래퍼가 있으면 사용). 저장 트리거는 `onDisappear` 일괄 저장 — 화면 이탈 시 PUT 1회.

- [ ] **Step 4: 진입점 연결**

Run: `grep -rn "MyPageView" dugout-ios --include=*.swift -l`
MyPageView 정의 모듈 확인. App 타겟이면 `NavigationLink { NotificationSettingsView() }` 를 MyPage 설정 목록에 추가. Feature 모듈이면 App 타겟에서 접근 불가하므로, MainTabView의 `.my` 케이스를 `NavigationStack(path: $router.myPath) { MyPageView(...) }` 로 감싸고 MyPage가 `navigationDestination` 으로 "notificationSettings" 라우트를 띄우도록 콜백/route enum 추가. 가장 단순한 경로: MainTabView의 `.my` 를 `NavigationStack { MyPageView(authViewModel: authViewModel) }` 로 감싸고, MyPageView 내 "알림 설정" 행이 App 타겟 화면을 못 여는 문제는 App 타겟에 진입 라우트를 두어 해결. 구체 방식은 MyPageView 구조 확인 후 이 Task에서 결정(3-C에서 PrimingView 위치를 코드 확인 후 정한 것과 동일 방침).

- [ ] **Step 5: 빌드 + 커밋**

Run: `make ios-build`
Expected: BUILD SUCCEEDED, warnings 0.

```bash
git add dugout-ios/Core/DesignSystem/Sources/Components/DGToggle.swift \
        dugout-ios/App/Sources/Notifications/NotificationPreferenceRepository.swift \
        dugout-ios/App/Sources/Notifications/NotificationSettingsViewModel.swift \
        dugout-ios/App/Sources/Notifications/NotificationSettingsView.swift \
        dugout-ios/App/Sources/MainTabView.swift
git commit -m "feat(ios): 알림 설정 화면 + DGToggle (Phase 3-D-4 4/4)"
```

---

## Milestone 5 — 문서 동기화 + 통합 검증

### Task 5.1: docs/TDD.md §4 갱신

**Files:**
- Modify: `docs/TDD.md`

- [ ] **Step 1: §4-2 이벤트 플로우에 신규 이벤트 3종 추가**

기존 "Phase 3-C 범위는 라인업 확정 broadcast 1종" 문구를 갱신하고, `MatchCreatedEvent`(매치 등록), `AttendanceChangedEvent`(출석 변경, 주장·가용성 경계만), `AttendanceReminderScheduler`(@Scheduled 매시, 48h/24h, per-user `attendance_reminder_logs` 멱등성) 플로우를 추가. NotificationType enum이 발송 분기/deeplink/설정 키를 통일한다는 점 명시.

- [ ] **Step 2: §4 알림 설정 단락 추가**

`notification_preferences` 테이블(유형별 4 boolean + dnd_enabled/start/end), GET/PUT API, 발송 경로 gating, DnD는 ATTENDANCE_REMINDER만 연기(즉시성 3종 무시) 규칙 기술.

- [ ] **Step 3: §2-3 DB 스키마에 2테이블 추가**

`attendance_reminder_logs`, `notification_preferences` 스키마 표 추가.

### Task 5.2: docs/PRD.md F9 매트릭스 갱신

**Files:**
- Modify: `docs/PRD.md`

- [ ] **Step 1: 알림 매트릭스 푸시 ✅ 채우기**

새 경기 일정 ✅ / 출석 리마인드(48h·24h) ✅ / 라인업 확정 ✅ / 출석 변경(주장) ✅. 알림 설정(유형별 on/off + DnD) 제공 명시. deeplink(푸시 탭 → 경기 상세) 추가.

- [ ] **Step 2: 문서 커밋**

```bash
git add docs/TDD.md docs/PRD.md
git commit -m "docs: TDD §4 / PRD F9 — 알림 완성(발송확대·리마인드·설정·deeplink) 반영 (Phase 3-D 문서)"
```

### Task 5.3: 통합 빌드 + PII/크리덴셜 가드 점검

- [ ] **Step 1: 전체 빌드**

```bash
make api-test
make ios-build
```
둘 다 통과.

- [ ] **Step 2: PII / 토큰 로그 노출 점검**

Run: `grep -rEn "(print|log\.(info|debug|warn|error)|os_log).*(fcmToken|\\.token|nickname|phone)" dugout-api/src/main/kotlin dugout-ios/App`
Expected: raw token/실명/연락처 로그 0줄. (스케줄러 log.info는 match/window/count만 — PII 없음 확인.)

- [ ] **Step 3: NotificationType drift 점검 (BE ↔ iOS)**

Run: `grep -rn "MATCH_CREATED\|LINEUP_CONFIRMED\|ATTENDANCE_REMINDER\|ATTENDANCE_CHANGED" dugout-api/src/main dugout-ios/App`
Expected: 백엔드는 enum 선언 + iOS는 rawValue 선언만. 4종이 양쪽에 1:1.

### Task 5.4: 수동 검증 시나리오 (CONTROLLER 수동, 자동 task 아님)

> 실기기 2대(주장 1 + 멤버 1) + `FCM_ENABLED=true`.

- [ ] 매치 등록 → 멤버 디바이스 "새 경기 일정" 수신 → **탭 → 경기 상세 착지**(foreground)
- [ ] 앱 강제 종료 후 푸시 탭 → cold-start 경기 상세 착지(DeepLinkInbox pending 소비)
- [ ] 멤버가 출석 ATTEND→ABSENT → 주장 "출석 응답이 변경됐어요" 수신. ATTEND→LATE 는 미수신(가용성 유지)
- [ ] 리마인드: 경기 시작시각을 now+24h 버킷에 맞춘 더미 경기 + 미응답자 → cron 수동 트리거(또는 시각 조정) → 1회 수신, 재실행 시 미수신(멱등성)
- [ ] 설정에서 "출석 리마인드" off → 리마인드 미수신, 다른 알림 정상
- [ ] DnD 22-08 + 야간 리마인드 due → 그 시각 미수신, 08시 이후 첫 정시에 수신

**문제 발견 시:** 새 task 추가 말고 plan 중단, controller 보고.

### Task 5.5: 머지 + push (CONTROLLER 수동)

```bash
git checkout main
git merge --no-ff feature/phase3-d-notification-spec -m "Merge branch 'feature/phase3-d-notification-spec'

Phase 3-D 완료 (알림 완성):
- NotificationType enum 표준화 + iOS deeplink(DeepLinkInbox + schedulePath)
- MatchCreatedEvent / AttendanceChangedEvent(주장·가용성 경계) broadcast
- 출석 리마인드 cron(48h/24h, per-user 멱등성 로그)
- NotificationPreference(유형별 on/off + DnD 22:00-08:00 리마인드 연기) + iOS 설정 화면"
git branch -d feature/phase3-d-notification-spec
git push origin main
```

---

## 검증 체크리스트

- [ ] `make api-test` 성공 (신규 테스트 AttendanceStatusTest 5 + NotificationPreferenceTest 5 포함)
- [ ] `make ios-build` 성공 (warnings 0)
- [ ] NotificationType 4종 BE↔iOS 1:1, payload type 하드코딩 0
- [ ] 푸시 탭 deeplink: foreground + cold-start 둘 다 경기 상세 착지
- [ ] 매치 등록 / 출석 변경(주장·가용성 경계만) broadcast 동작
- [ ] 리마인드 cron: 48h/24h, 미응답자만, voteDeadline·비SCHEDULED skip, per-user 멱등성
- [ ] 알림 설정 GET/PUT + lazy 기본값, 유형별 off 시 미발송
- [ ] DnD: 리마인드만 연기(자정 넘김 판정), 즉시성 3종은 무시
- [ ] 마이그레이션 V2/V3 적용, BaseEntity 컬럼 규약 일치
- [ ] PII/토큰 로그 노출 0
- [ ] docs/TDD §4 + §2-3, docs/PRD F9 갱신
- [ ] 커밋 단위(서브-phase별) + main 머지 + push

---

## 후속 Phase 후보 (본 plan 비범위)

- 전체 `NotificationLog` 감사 엔티티 (현재는 리마인드 전용 로그만)
- topic 기반 발송, 환경별 GoogleService-Info.plist 분리
- 회비/매칭/용병 알림, 이메일 채널
- 마이페이지 권한 재요청 UX
- deeplink 세분화 (라인업 화면 직접 착지 등)
