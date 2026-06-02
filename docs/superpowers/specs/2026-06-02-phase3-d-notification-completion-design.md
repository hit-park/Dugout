# Phase 3-D 설계: 알림 완성 (Notification Completion)

> Phase 3-C(FCM 인프라 + 라인업 확정 broadcast 1종) 위에 알림 기능을 완성한다.
> 발송 확대 → 제어 순서로 4개 서브-phase. 기존 `LineupConfirmedEvent` + `@TransactionalEventListener(AFTER_COMMIT)` 패턴을 재사용한다.

**선행 기준선 (Phase 3-C 완료분):**
- `global/fcm/FcmClient.sendToTokens(tokens, message)` — Firebase Admin SDK 단일 래퍼, `fcm.enabled=false` stub
- `domain/notification/` (controller/service/dto/event) + `users.fcm_token` 컬럼
- `LineupConfirmedEvent` → `NotificationService.onLineupConfirmed` (AFTER_COMMIT) → multicast
- iOS: `AppDelegate` + `actor PushPermissionCoordinator` + 권한 priming + 로그아웃 토큰 정리
- iOS: `AppRouter`(@MainActor @Observable) — 탭별 `NavigationPath`(`schedulePath` 등) 보유

---

## 1. 목표와 범위

### 목표
방금 도입한 FCM 인프라가 "라인업 확정 푸시 발송"만 하고 있다. 다음 셋을 완성한다.
1. 이미 나가는 푸시를 **탭하면 해당 화면으로 이동**하게 한다 (현재 `didReceive`가 비어 있어 반쪽).
2. 알림 **종류를 늘린다** (매치 등록, 출석 변경, 출석 리마인드).
3. 사용자가 알림을 **끌 수 있게 한다** (유형별 on/off + 방해금지 시간).

### 서브-phase (구현 순서)
| 서브 | 범위 | 규모 |
|------|------|------|
| 3-D-1 | `NotificationType` enum 표준화 + iOS deeplink 라우팅 | S |
| 3-D-2 | `MatchCreatedEvent` / `AttendanceChangedEvent` broadcast | S |
| 3-D-3 | 출석 리마인드 cron (`@Scheduled`, 48h/24h, 멱등성) | M |
| 3-D-4 | `NotificationPreference` (유형별 on/off + DnD) + iOS 설정 화면 | M~L |

### 비범위 (후속 Phase)
- 전체 `NotificationLog` 감사 엔티티 (3-D-3은 경량 리마인드 로그만)
- topic 기반 발송, 환경별 `GoogleService-Info.plist` 분리
- 회비/매칭/용병 알림, 이메일 채널
- (영구 제외) 카카오 알림톡

### 확정된 product 결정
- **출석 변경 알림**: 주장(`TeamRole.CAPTAIN`)에게만, 라인업 가용성을 바꾸는 변경만
- **리마인드**: 미응답자(Attendance row 없음)에게만, 48h + 24h
- **DnD**: 기본 22:00–08:00, **리마인드만 연기**. 즉시성 알림(매치등록/라인업확정/출석변경)은 DnD 무시

---

## 2. 3-D-1 — NotificationType 표준화 + deeplink

### 2-1. NotificationType (계약 단일화)
현재 `NotificationService` 가 `"type" to "LINEUP_CONFIRMED"` 문자열을 하드코딩한다(payload data map). 이 문자열은 곧 (a) deeplink 라우팅 키, (b) 설정 토글 키, (c) 발송 분기 키 세 곳에서 쓰인다. 단일 enum으로 승격해 drift를 막는다.

**백엔드** `domain/notification/NotificationType.kt`:
```
enum class NotificationType { MATCH_CREATED, LINEUP_CONFIRMED, ATTENDANCE_REMINDER, ATTENDANCE_CHANGED }
```
- `FcmMessage.data` 의 `type` 은 `NotificationType.X.name` 으로만 채운다.
- 공통 헬퍼 `notificationData(type, matchId, teamId, lineupId?)` 로 data map 생성을 일원화.

**iOS** `App/Sources/Notifications/NotificationType.swift`:
- `enum NotificationType: String, Sendable` — rawValue가 백엔드 `.name` 과 1:1.

### 2-2. payload 파싱 + 라우팅
**문제.** `PushPermissionCoordinator` 는 `AppDelegate` 가 만드는 `actor .shared` 이고, `AppRouter` 는 `DugoutApp` 의 `@State` 다 → actor가 router를 직접 못 잡는다. 또 앱이 종료된 상태에서 푸시 탭 시 router가 아직 없을 수 있다(cold-start).

**해법 — DeepLinkInbox (버퍼링 브리지).**
```
@MainActor @Observable
final class DeepLinkInbox {
    static let shared = DeepLinkInbox()
    var pending: PushRoute?
}
struct PushRoute: Sendable { let type: NotificationType; let matchId: Int64? }
```
- `PushPermissionCoordinator.userNotificationCenter(didReceive:)` (현재 비어 있음) → `userInfo` 를 `PushPayload` 로 파싱 → `await MainActor.run { DeepLinkInbox.shared.pending = route }`.
- `MainTabView` 가 `.onChange(of: inbox.pending)` 및 `.task`(cold-start) 로 소비 → `AppRouter` 에 반영 후 `pending = nil`.
- 반영 로직: `router.selectedTab = .schedule; router.schedulePath = NavigationPath([matchId])`. 모든 type은 우선 `MatchDetail(matchId)` 로 착지(거기서 라인업 진입 가능). `matchId == nil` 이면 탭 전환만.

> 대안(기각): `AppRouter` 를 싱글톤화. 타입 수는 줄지만 `@State` 소유 모델을 깨고 테스트성이 떨어진다.

```
struct PushPayload: Decodable {
    let type: NotificationType
    let matchId: Int64?
    let teamId: Int64?
    let lineupId: Int64?
}
```

### 2-3. 에러 처리
- payload 파싱 실패 → 라우팅 없이 앱만 열림(현재 동작 유지), `print` 로그. 크래시 금지.
- `MainActor.run` 홉의 캡처값(`PushRoute`)은 `Sendable`.

---

## 3. 3-D-2 — 발송 확대 (이벤트 broadcast)

기존 `LineupConfirmedEvent` 패턴 그대로 복제. `XxxService` 는 이벤트만 발행하고 `NotificationService` 가 consumer (의존 역전 유지, 이벤트 클래스는 `domain/notification/event/` 에 둔다).

### 3-1. MatchCreatedEvent
- `data class MatchCreatedEvent(matchId, teamId, createdBy)`.
- 발행: `MatchService.createMatch` 의 트랜잭션 내, return 직전.
- 리스너 `onMatchCreated`(AFTER_COMMIT): 활성 팀원 − 생성자 → "새 경기 일정이 등록됐어요" (body: 날짜·구장·상대팀). type=`MATCH_CREATED`.

### 3-2. AttendanceChangedEvent
- `data class AttendanceChangedEvent(matchId, teamId, actorUserId, previous: AttendanceStatus, new: AttendanceStatus)`.
- 발행: `AttendanceService.updateVote` 에서만 (첫 `vote` 는 "변경" 아님). `previous` 는 갱신 전 status.
- 리스너 `onAttendanceChanged`(AFTER_COMMIT):
  - **대상**: 해당 팀의 `TeamRole.CAPTAIN` 멤버 1인 (`TeamMemberRepository` 로 조회). 변경한 사람이 주장 본인이면 skip.
  - **의미있는 변경만**: `AVAILABLE = {ATTEND, LATE, EARLY_LEAVE}`, `UNAVAILABLE = {ABSENT, MAYBE}`. `(previous in AVAILABLE) != (new in AVAILABLE)` 일 때만 발송(가용성 경계 통과). 동일군 내 변경(MAYBE↔ABSENT, ATTEND↔LATE 등)은 무시.
  - body 예: "홍**님이 불참으로 바꿨어요 · 5월 20일 vs FC서울" (PII 마스킹: 닉네임 표시 정책은 기존 도메인 표시 규칙 따름, 로그엔 미노출).
  - type=`ATTENDANCE_CHANGED`.

### 3-3. 에러 처리
- 주장에 fcmToken 없으면 조용히 종료(발송 0). FCM 오류는 로그만, 비즈니스 트랜잭션 무영향(3-C와 동일). UNREGISTERED 토큰은 기존 `cleanUpInvalidTokens` 재사용.

---

## 4. 3-D-3 — 출석 리마인드 cron

### 4-1. 스케줄러
- `AttendanceReminderScheduler` `@Scheduled`(매시 정각, `fixedDelay`/cron). 별도 `@Async` 불필요(짧음).
- window 2종: 48h, 24h. 각 실행에서 "지금부터 window 시간 뒤 1시간 버킷에 시작하는 경기" 조회.
- **Match는 `matchDate: LocalDate` + `matchTime: LocalTime` 분리 저장** → 시작시각 = `LocalDateTime.of(matchDate, matchTime)`.
- `MatchRepository` 신규: `LocalDateTime` 범위 쿼리(`@Query` 로 `matchDate`/`matchTime` 조합 비교, 또는 `matchDate` 범위로 1차 필터 후 in-memory 시각 비교). 현재는 `LocalDate` 범위 쿼리만 있음.

### 4-2. 대상 산출
- 미응답자 = 활성 팀원 − 해당 match에 Attendance row 있는 사용자.
- `AttendanceRepository` 신규: `findRespondedUserIds(matchId): List<Long>` → 팀원 집합과 차집합.

### 4-3. skip 조건
- `match.status != MatchStatus.SCHEDULED` → skip
- `match.voteDeadline != null && now > voteDeadline` → skip

### 4-4. 멱등성 — `attendance_reminder_log` (경량 테이블)
```
attendance_reminder_log
  id            PK
  match_id      FK
  user_id       FK
  reminder_window  ENUM/INT  (H48 | H24)
  sent_at       timestamp
  UNIQUE (match_id, reminder_window, user_id)
```
- **per-user 키인 이유**: 3-D-4의 DnD가 붙으면, 야간(22–08) 미응답자는 그 시각엔 skip하고 **로그를 남기지 않아** 다음 eligible 시간대(아침 08시 이후 실행)에 재시도 → "아침으로 연기"가 자연 구현된다. 발송 성공 시에만 로그 → 중복 발송 0.
- 3-D-3 시점(설정 없음): DnD 분기 없이 모든 미응답자에게 window 시각에 발송 + 로그. 3-D-4가 DnD 분기를 이 루프에 추가.
- full `NotificationLog`(전 유형 감사)는 여전히 후속 Phase. 이 테이블은 리마인드 전용.

### 4-5. 에러 처리
- 토큰 없는 미응답자는 로그도 남기지 않음(다음 실행 재시도해도 무의미하나 발송 시도 자체를 skip). FCM 실패 시 해당 user 로그 미기록 → 재시도 허용. UNREGISTERED → 토큰 정리.

---

## 5. 3-D-4 — 알림 설정 (NotificationPreference + DnD)

### 5-1. 백엔드 모델
- `NotificationPreference` 엔티티 (`user` 1:1, `BaseEntity` 상속, soft delete 규약 따름):
  - 유형별 boolean 4: `matchCreated`, `lineupConfirmed`, `attendanceReminder`, `attendanceChanged` (기본 전부 `true`)
  - `dndEnabled: Boolean` (기본 `true`), `dndStart: LocalTime`(22:00), `dndEnd: LocalTime`(08:00)
- 신규 유저는 row 없을 수 있음 → **조회 시 없으면 기본값 객체로 취급**(lazy 생성 or 기본값 fallback).
- API:
  - `GET /api/v1/users/me/notification-preferences` → 현재 설정(없으면 기본값)
  - `PATCH /api/v1/users/me/notification-preferences` → 부분 갱신 (Request DTO 모든 필드 nullable, 누락 필드는 보존). iOS 는 현재 항상 full DTO 를 보내 effective overwrite 동작.
  - 새 ErrorCode 필요시 추가(기존 `USER_NOT_FOUND` 재사용).

### 5-2. 발송 경로 gating (모든 listener 공통)
- `NotificationService` 가 수신자 token 수집 **전에** 각 수신자 preference로 필터:
  - 해당 `NotificationType` 의 유형 toggle이 `false` 인 사용자 제외.
  - `ATTENDANCE_REMINDER` 만 추가로 DnD 적용: 발송 시각이 사용자 DnD 구간이면 이번엔 skip(로그 미기록 → 재시도). 즉시성 3종은 DnD 무시.
- `NotificationPreferenceRepository.findByUserIdIn(ids): List<NotificationPreference>` 신규 → 일괄 조회 후 map.
- DnD 구간 판정: `dndStart > dndEnd`(자정 넘김, 22→08) 케이스를 올바로 처리.

### 5-3. iOS
- **`DGToggle` 신규** (DesignSystem에 토글/스위치 컴포넌트 없음 — `DGSegmentedControl` 만 존재). `Core/DesignSystem/Sources/Components/DGToggle.swift`.
- `NotificationSettingsView`: 유형별 토글 4 + DnD 토글 + 시작/종료 `DatePicker`(.hourAndMinute). MyPage에서 진입.
- `NotificationPreferenceRepository`(GET/PATCH) + `NotificationSettingsViewModel`(`@MainActor final class ... ObservableObject`).
- DTO → Domain Entity 변환(Codable 직접 노출 금지) — Feature Clean Architecture 규약.

### 5-4. 에러 처리
- 설정 로드 실패 → 기본값으로 화면 표시 + `errorMessage`. 저장 실패 → 토스트/에러 상태, 로컬 상태 롤백.

---

## 6. 데이터 모델 변경 요약

| 변경 | 종류 |
|------|------|
| `attendance_reminder_log` 테이블 + 엔티티 | 신규 |
| `notification_preference` 테이블 + 엔티티 | 신규 |
| `MatchRepository` LocalDateTime 범위 쿼리 | 신규 메서드 |
| `AttendanceRepository.findRespondedUserIds` | 신규 메서드 |
| `NotificationType` enum (BE/iOS) | 신규 |
| DB 마이그레이션 2건 (reminder_log, notification_preference) | 신규 |

문서 동기화: `docs/TDD.md §4`(이벤트 플로우 + 스케줄러 + 설정 + 2테이블 스키마), `docs/PRD.md F9`(알림 매트릭스 ✅ 채움), `dugout-ios` 디자인 가이드(`DGToggle`).

---

## 7. 테스트 / 검증

- 단계마다 `make api-test` / `make ios-build`(warnings 0).
- **멱등성**: 동일 (match, window, user) 재실행 시 발송 0 (unique 제약 + 로그 선조회).
- **DnD 경계**: 자정 넘김(22→08) 판정, 즉시성 알림은 DnD 무시 확인.
- **의미있는 변경 필터**: AVAILABLE↔UNAVAILABLE 경계만 발송, 동일군 변경 무시.
- **PII 가드**: raw fcm token / 실명·연락처 로그 노출 0. 픽스처 가상 데이터만.
- **수동 시나리오(실기기)**: 푸시 탭 → MatchDetail 착지(cold/foreground), 매치 등록 알림 수신, 주장 출석변경 알림, 리마인드 cron(시각 조정), 설정 off 시 미수신, DnD 야간 리마인드 아침 수신.

---

## 8. 확정된 가정 (코드 검증 완료)

- 주장 식별: `TeamRole.CAPTAIN` 존재 (`domain/team/entity/TeamRole.kt`), `TeamMember.role`. → 3-D-2 출석변경 대상 확정.
- `DGToggle` 부재 확인 → 3-D-4에서 신규 작성.
- `AppRouter` 탭별 `NavigationPath` 존재 → deeplink 착지는 `selectedTab` + `schedulePath` 설정으로 충분.
- Attendance status 5종(ATTEND/ABSENT/MAYBE/LATE/EARLY_LEAVE), `vote`/`updateVote` 분리 → 변경 이벤트는 `updateVote`에만.
