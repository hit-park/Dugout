# Phase 3-C Design: FCM 푸시 알림 인프라 (카카오 알림톡 제외)

> 작성일: 2026-05-27
> 범위: S (인프라만) — FCM 토큰 등록/갱신 + 라인업 확정 broadcast 1종
> 비범위: 매치 등록/출석 리마인드/알림 설정 화면/deeplink/topic/카카오 알림톡(영구 제외)

## 1. 배경 & 결정

### 1.1 카카오 알림톡 제외 사유

카카오 비즈메시지 알림톡은 발송당 9~15원의 사용량 과금이 발생한다. 사용자 규모가 커질수록 비용이 선형 증가하며, 비즈니스 채널 심사 / 템플릿 사전 등록 / 사업자 등록 등 운영 부담도 크다. Phase 3-C 단계에서는 다음 두 가지 이유로 알림톡을 영구 스코프 외로 둔다:

- **무료 도달성**: FCM 은 발송량 무제한 무료. 앱 설치자에 대해서는 카카오톡과 동일한 도달성 확보
- **앱 미설치자 도달 포기**: 카카오톡의 가장 큰 장점이지만, MVP 단계에서는 앱 설치 사용자의 알림 경험에 집중

이 결정은 PRD F9 의 "카카오 알림톡 전략" 단락을 무효화하며, 본 spec 의 일부로 PRD/TDD/DB 스키마를 동시에 정리한다.

### 1.2 FCM 만으로 충분한 이유

- **비용**: 발송 0원, 디바이스/토픽 수 한도 없음, Spark plan 무료
- **표준성**: Firebase Admin SDK 가 안정적이고 Kotlin/JVM 지원 완성도 높음
- **Android 대비**: Phase 2 Android 앱 추가 시 동일 인프라 재사용
- **회수 가능**: 후속 Phase 에서 카카오 알림톡을 다시 검토할 여지 있음 (현 spec 의 결정이 영구 제거하지는 않음. 다만 DB 컬럼은 drop 하므로 부활 시 재추가 필요)

### 1.3 본 Phase 의 범위 (S — 인프라만)

| 포함 | 비포함 |
|---|---|
| iOS 푸시 권한 요청 + priming view | 알림 설정 화면 (유형별 on/off) |
| FCM 토큰 발급 + 백엔드 등록 endpoint | DnD (방해 금지) 시간 |
| 토큰 회전 / 로그아웃 시 정리 | 알림 탭 → deeplink 화면 이동 |
| 라인업 확정 broadcast 1종 (이벤트 기반) | 매치 등록 broadcast |
| application.yml FCM 설정 | 출석 응답 변경 broadcast |
| 카카오 흔적 제거 (DB / 문서) | 출석 리마인드 cron 스케줄러 |
|  | NotificationLog 감사 엔티티 |
|  | Topic 기반 발송 (token loop 만) |

## 2. 백엔드 (dugout-api)

### 2.1 패키지 구조

```
src/main/kotlin/com/dugout/api/
├── domain/notification/
│   ├── controller/
│   │   └── NotificationController.kt          (PATCH /api/v1/users/me/fcm-token)
│   ├── service/
│   │   └── NotificationService.kt             (다른 도메인이 호출하는 진입점)
│   ├── dto/
│   │   ├── FcmTokenRequest.kt
│   │   └── FcmTokenResponse.kt
│   └── event/
│       └── LineupConfirmedEvent.kt            (ApplicationEvent data class)
├── global/fcm/
│   ├── FcmClient.kt                            (Firebase Admin SDK 단일 래퍼)
│   ├── FcmConfig.kt                            (@Configuration — FirebaseApp init)
│   └── FcmMessage.kt                           (내부 payload struct)
└── global/error/ErrorCode.kt                   (NOTIFICATION_TOKEN_INVALID 추가)
```

5폴드 일관성을 위해 `entity/`, `repository/` 폴더는 본 Phase 에서 비워둔다. `NotificationLog` 엔티티는 후속 Phase 의 감사 요구사항이 생긴 시점에 추가한다.

### 2.2 API 엔드포인트

#### PATCH `/api/v1/users/me/fcm-token`

| 필드 | 타입 | 비고 |
|---|---|---|
| Authorization | Bearer JWT | 필수 |
| Body | `{ "token": "string?" }` | null 허용 (로그아웃/거부) |
| Response 200 | `{ "ok": true }` | idempotent |
| Error 400 | `NOTIFICATION_TOKEN_INVALID` | 비어있지 않은데 유효 패턴 아닐 때 |

동작:
- 인증된 사용자의 `users.fcm_token` 컬럼 갱신
- null 또는 빈 문자열 → 컬럼 null 처리 (로그아웃 / 권한 해제 / 앱 삭제 신호)
- 같은 토큰을 반복 PATCH 해도 무해

### 2.3 도메인 간 이벤트 흐름

```
LineupController.confirmLineup(matchId, principal)
  └─ LineupService.confirmLineup(matchId, userId)
       @Transactional
       ├─ Lineup.isConfirmed = true
       ├─ applicationEventPublisher.publishEvent(
       │     LineupConfirmedEvent(lineupId, matchId, teamId, confirmedBy = userId)
       │   )
       └─ return Lineup
                                ↓ commit 후
NotificationService
  @TransactionalEventListener(phase = AFTER_COMMIT)
  fun onLineupConfirmed(e: LineupConfirmedEvent) {
      val members = teamMemberRepository.findByTeamId(e.teamId)
      val tokens = members
          .filter { it.userId != e.confirmedBy }       // 주장 본인 제외
          .mapNotNull { it.user.fcmToken }
      if (tokens.isEmpty()) return
      val match = matchRepository.findById(e.matchId).orElseThrow()
      val payload = buildLineupConfirmedPayload(match)
      val result = fcmClient.sendToTokens(tokens, payload)
      cleanUpInvalidTokens(tokens, result)              // UNREGISTERED → fcmToken=null
  }
```

설계 원칙:
- **`LineupService` 는 `NotificationService` 를 import 하지 않는다**. 의존 역전.
- **AFTER_COMMIT** — 라인업 저장 트랜잭션이 성공적으로 commit 된 후에만 발송. 알림 실패가 비즈니스 트랜잭션 rollback 을 일으키지 않는다.
- **early return** — 토큰 없는 팀이면 FCM 호출 skip.

### 2.4 FCM payload 구조

```json
{
  "notification": {
    "title": "라인업이 확정됐어요",
    "body": "5월 27일 (수) · 잠실구장 · vs 베어스"
  },
  "data": {
    "type": "LINEUP_CONFIRMED",
    "matchId": "42",
    "teamId": "7",
    "lineupId": "13"
  },
  "apns": {
    "payload": { "aps": { "sound": "default", "badge": 1 } }
  }
}
```

- `notification` 블록 → iOS 시스템이 백그라운드/잠금 화면에서 자동 표시
- `data` 블록 → 앱이 직접 파싱 (현 Phase 에서는 사용 X, deeplink 후속 Phase 용)
- `apns.payload.aps` → iOS 전용 옵션 (sound, badge)

### 2.5 발송 방식: token loop (multicast)

Firebase Admin SDK 의 `MulticastMessage` + `sendEachForMulticast` 사용. 한 번 호출에 500 token 까지. Dugout 평균 팀 규모 (15~30명) 기준 1회 호출로 충분.

```kotlin
class FcmClient(private val firebaseMessaging: FirebaseMessaging?) {
    fun sendToTokens(tokens: List<String>, payload: FcmMessage): BatchResult {
        if (tokens.isEmpty()) return BatchResult.empty()
        if (firebaseMessaging == null) return BatchResult.stub() // FCM_ENABLED=false

        val message = MulticastMessage.builder()
            .setNotification(Notification.builder()
                .setTitle(payload.title)
                .setBody(payload.body)
                .build())
            .putAllData(payload.data)
            .setApnsConfig(/* sound, badge */)
            .addAllTokens(tokens)
            .build()
        val response = firebaseMessaging.sendEachForMulticast(message)
        return BatchResult.from(tokens, response)
    }
}
```

### 2.6 실패 처리

| FCM 응답 | 처리 |
|---|---|
| 성공 | 무동작 |
| `UNREGISTERED` | `users.fcm_token = null` (앱 삭제) |
| `INVALID_ARGUMENT` | `users.fcm_token = null` (토큰 손상) |
| `QUOTA_EXCEEDED` / 네트워크 오류 | 로그만 (info 레벨), swallow |
| `SENDER_ID_MISMATCH` | error 로그 + 알림 (운영 설정 문제) |

알림 미발송은 비즈니스 critical 하지 않으므로 어떤 FCM 오류도 트랜잭션 rollback 시키지 않는다.

### 2.7 DB 마이그레이션

```sql
-- V<N>__drop_kakao_alimtalk_agreed.sql
ALTER TABLE users DROP COLUMN kakao_alimtalk_agreed;
```

User 엔티티에서 `kakaoAlimtalkAgreed: Boolean` 필드 제거. `fcmToken: String?` 은 그대로 유지하고 본 Phase 에서 처음 활용한다.

### 2.8 application.yml 추가

```yaml
fcm:
  enabled: ${FCM_ENABLED:false}
  credentials-path: ${FCM_CREDENTIALS_PATH:}
  project-id: ${FCM_PROJECT_ID:}
```

- 로컬 개발 기본값 `enabled=false` — `FcmClient` 가 stub 응답 반환, FirebaseApp 초기화도 skip
- 운영 환경에서 service account JSON 파일 경로를 env 로 주입
- **service account JSON 은 git 절대 X**. 보안 가드 (CLAUDE.md) 준수

### 2.9 새 ErrorCode

| 코드 | HTTP | 메시지 |
|---|---|---|
| `NOTIFICATION_TOKEN_INVALID` | 400 | "FCM 토큰 형식이 올바르지 않습니다" |

FCM 발송 실패 자체는 별도 ErrorCode 를 만들지 않는다. 비즈니스 응답에 영향 주지 않는 흐름이므로.

## 3. iOS (dugout-ios)

### 3.1 Tuist 의존성

`Tuist/Package.swift` 에 SPM 추가:

```swift
.package(url: "https://github.com/firebase/firebase-ios-sdk", from: "11.0.0")
```

`Project.swift` 의 App 타겟 dependencies 에만 link:

```swift
.external(name: "FirebaseMessaging")
```

다른 Firebase 제품(Analytics / Crashlytics / Firestore / Authentication) 은 link 하지 않는다 — Spark plan 무료 유지.

### 3.2 신규 파일

```
App/Sources/
├── DugoutApp.swift                              (기존 — AppDelegate adapter 추가)
├── AppDelegate.swift                            (신규 — APNs/FCM lifecycle)
└── Notifications/
    ├── PushPermissionCoordinator.swift          (actor — 권한 요청 + token sync)
    ├── PushPermissionPrimingView.swift          (DGCard 기반 priming UI)
    └── FCMTokenStore.swift                      (actor — 마지막 동기화 token 캐시)
```

Feature 모듈은 알림 인프라를 모른다. App 타겟 안에서만 구현한다.

### 3.3 GoogleService-Info.plist

- 위치: `dugout-ios/App/Resources/GoogleService-Info.plist`
- Firebase 공식 가이드에 따라 public client config (secret 아님) → gitignore 하지 않음
- 환경별 분리 (dev/prod) 는 본 Phase 비범위 — 단일 Firebase 프로젝트 사용
- README 에 Firebase 콘솔에서 받는 절차 명시

### 3.4 AppDelegate

```swift
import FirebaseCore
import FirebaseMessaging
import UIKit

@MainActor
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = PushPermissionCoordinator.shared
        UNUserNotificationCenter.current().delegate = PushPermissionCoordinator.shared
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }
}
```

`DugoutApp.swift`:

```swift
@main
struct DugoutApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    // ... 기존 내용 유지
}
```

### 3.5 PushPermissionCoordinator (actor)

```swift
actor PushPermissionCoordinator: NSObject {
    static let shared = PushPermissionCoordinator()

    private let repository: any NotificationRepository
    private var lastSyncedToken: String?

    func requestAuthorization() async -> Bool { /* UNUserNotificationCenter */ }
    func handleNewToken(_ token: String?) async { /* PATCH /fcm-token */ }
    func clearToken() async { /* PATCH null */ }
}

extension PushPermissionCoordinator: MessagingDelegate {
    nonisolated func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        Task { await self.handleNewToken(fcmToken) }
    }
}

extension PushPermissionCoordinator: UNUserNotificationCenterDelegate {
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
```

Swift 6 strict concurrency 준수:
- `actor` 로 mutable state 격리
- delegate callback 은 `nonisolated` + `Task { await ... }` 패턴
- `static let shared` 는 actor 라 자동 Sendable

### 3.6 PushPermissionPrimingView (DGCard 기반)

```
┌─────────────────────────────────┐
│         🔔 (큰 아이콘)           │
│                                  │
│      알림을 받아볼까요?           │
│                                  │
│   ⚾ 경기 일정 등록              │
│   📋 라인업 확정                 │
│   ⏰ 출석 응답 리마인드          │
│                                  │
│   [ 알림 허용 ] (primary)        │
│   [ 나중에 ] (tertiary)          │
└─────────────────────────────────┘
```

호출 시점은 OAuth 콜백 성공 → MainTabView 진입 직전. 시스템 권한 상태가 `.notDetermined` 일 때만 표시.

### 3.7 권한 분기 흐름

```
OAuth 콜백 성공 → AuthViewModel.signInSuccess
        ↓
UNUserNotificationCenter.notificationSettings().authorizationStatus
   ├─ .notDetermined  → PushPermissionPrimingView 표시
   │      ├─ [알림 허용] → requestAuthorization()
   │      │       ├─ 허용 → registerForRemoteNotifications()
   │      │       │         → APNs token → FCM token → PATCH /fcm-token
   │      │       └─ 거부 → skip
   │      └─ [나중에] → skip
   ├─ .authorized      → registerForRemoteNotifications() (이미 권한, token 재발급)
   └─ .denied          → 무시 (마이페이지에서 설정 진입 안내는 후속 Phase)
        ↓
MainTabView 진입
```

priming view 에서 "나중에" 를 선택한 경우 본 Phase 에서는 다시 묻지 않는다. 마이페이지에서 알림 설정으로 재요청하는 흐름은 후속 Phase.

### 3.8 토큰 회전 처리

`messaging:didReceiveRegistrationToken:` 은 다음 상황에 자동 호출된다:
- 앱 첫 실행 시 1회
- 앱 재설치 / 데이터 클리어
- 백그라운드에서 토큰 회전 (드물지만 발생)

`PushPermissionCoordinator.handleNewToken()` 이 `lastSyncedToken` 과 비교하여 다를 때만 PATCH. 동일하면 중복 호출 방지.

### 3.9 로그아웃 처리

`AuthViewModel.signOut()` 에 추가:

```swift
await PushPermissionCoordinator.shared.clearToken()  // PATCH null
Messaging.messaging().deleteToken { _ in }            // 로컬 FCM token 폐기
```

다른 계정으로 다시 로그인하면 새 token 발급 + PATCH 가 자연스럽게 실행된다.

### 3.10 NotificationRepository

Repository 패턴 일관성을 위해 신설:

```swift
// App/Sources/Notifications/Domain/
public protocol NotificationRepository: Sendable {
    func patchFcmToken(_ token: String?) async throws
}

// 구현
final class NotificationRepositoryImpl: NotificationRepository {
    func patchFcmToken(_ token: String?) async throws {
        let endpoint = APIEndpoint(
            path: "/api/v1/users/me/fcm-token",
            method: .patch,
            body: ["token": token as Any?]
        )
        let _: EmptyResponse = try await client.request(endpoint)
    }
}
```

App 타겟 안에서만 사용되므로 별도 Feature 모듈로 분리하지 않는다.

## 4. 라인업 확정 broadcast 종단 시나리오

```
[주장 iOS]                  [dugout-api]              [FCM]              [멤버 iOS]
   │ POST /lineup/confirm     │                        │                    │
   ├─────────────────────────►│                        │                    │
   │                          │ Lineup.isConfirmed=true                     │
   │                          │ publishEvent(LineupConfirmedEvent)          │
   │ ◄── 200 LineupResponse ──┤                        │                    │
   │                          │ (AFTER_COMMIT)         │                    │
   │                          │ NotificationService    │                    │
   │                          │   tokens = members − 주장 본인              │
   │                          │   fcmClient.sendToTokens                    │
   │                          ├──────────────────────► │                    │
   │                          │   BatchResponse        │ ──► APNs ────────► │
   │                          │◄────────────────────── │   배너 표시         │
   │                          │   UNREGISTERED → null  │                    │
```

### 동작 규칙 요약

- 주장 본인은 발송 대상에서 제외
- token 없는 멤버는 자동 skip (filter)
- 빈 token 리스트면 FCM 호출 자체 안 함
- UNREGISTERED / INVALID_ARGUMENT 응답 → 해당 user 의 fcmToken null 처리
- 다른 FCM 오류 → 로그만, 비즈니스 응답에 영향 X
- 포그라운드 → iOS `willPresent` 에서 배너 표시
- 백그라운드/잠금 → 시스템 자동 표시
- 알림 탭 → 앱이 열리기만 함 (deeplink 비범위)

## 5. 보안 / 개인정보 가드

| 항목 | 규칙 |
|---|---|
| service account JSON | env 경로로만 주입, git/응답/문서 X |
| FCM token | raw 토큰 로그 출력 X — prefix 8자만 (`abc12345...`) |
| 알림 payload | nickname 노출 OK (팀원 간 이미 공개) |
| 알림 payload | 전화번호 / 이메일 / 카카오ID 절대 포함 X |
| GoogleService-Info.plist | public client config — git 가능 (Firebase 공식 가이드) |

## 6. 문서 동기화

본 Phase 머지 시 함께 갱신:

### 6.1 `docs/TDD.md`

- **section 4-1 카카오 알림톡 연동** → **FCM 통합** 으로 완전 교체 (Firebase Admin SDK 래퍼, payload 구조, application.yml 설정)
- **section 4-2 알림 발송 플로우** → **이벤트 기반 발송** 으로 교체 (TransactionalEventListener AFTER_COMMIT, 라인업 확정 broadcast 1종이 S 범위임을 명시)
- **section 2-3 DB 스키마** users 테이블에서 `kakao_alimtalk_agreed` 컬럼 제거
- DugoutMatchFeature / DugoutLineupFeature 소개 단락의 "카카오 알림톡·푸시 알림은 후속 Phase 예정" 문구를 "Phase 3-C 에서 FCM 푸시 도입, 카카오 알림톡은 비용/운영 부담으로 영구 제외" 로 갱신

### 6.2 `docs/PRD.md`

- **F9-1 알림 채널**: 카카오 알림톡 항목 제거, FCM (앱 푸시) 만 남김. 이메일은 그대로
- **F9-2 알림 매트릭스**: "알림톡" 열 삭제, "푸시" 열만 유지
- **"카카오 알림톡 전략" 단락** 완전 삭제. 대신 짧은 "FCM 우선 전략" 단락 추가 (앱 미설치자 도달 포기 명시)
- **F9 우선순위 P0 유지** (사용자 전환의 핵심 채널이라는 위치 자체는 그대로)

### 6.3 `dugout-api/README.md` / `dugout-ios/README.md`

Firebase 콘솔에서 service account JSON 및 GoogleService-Info.plist 를 받는 절차 추가.

## 7. 검증 전략

### 7.1 자동 검증

- `make api-test` — dugout-api 컴파일 점검, 회귀 없음 확인
- `make ios-build` — warnings 0
- 단위 테스트는 본 Phase 미작성 (테스트 인프라 자체가 Phase 2 예정)

### 7.2 수동 시뮬레이터 시나리오

1. 시뮬레이터 1대 (주장 iOS) + 시뮬레이터 2대 (멤버 iOS) 준비, 동일 팀에 가입
2. 3 디바이스 각각 OAuth 로그인 → priming view → 권한 허용 → 백엔드 로그에서 token PATCH 3회 확인
3. 주장이 라인업 추천 → 저장 → 확정
4. 멤버 시뮬레이터 2대에서 배너 수신 (백그라운드면 시스템 알림, 포그라운드면 in-app 배너)
5. 한 멤버 시뮬레이터 앱 삭제 → 주장이 라인업 재확정 → 백엔드 로그에서 UNREGISTERED 감지 + DB 의 해당 user.fcm_token = null 확인
6. `FCM_ENABLED=false` 로 백엔드 재기동 → 라인업 확정 정상 동작 + NotificationService 가 stub 호출 + 비즈니스 응답에 영향 없음 확인

### 7.3 PII 가드 점검

```bash
grep -rEn "(print|log\.|os_log).*(fcmToken|token)" \
  dugout-api/src/main/kotlin dugout-ios/App
# raw token 출력 0 라인 기대
```

## 8. 명시적 비범위 (다음 Phase 후보)

본 spec 에서는 **다루지 않으며**, 후속 Phase 에서 각각 별도 spec 으로 진행:

- 매치 등록 broadcast
- 출석 응답 변경 broadcast
- 출석 리마인드 cron 스케줄러 (48h / 24h 전)
- 알림 설정 화면 (유형별 on/off, DnD 시간)
- 알림 탭 → 화면 deeplink (`NotificationRouter`)
- `NotificationLog` 감사 엔티티
- topic 기반 발송 전환 (token loop → topic)
- 환경별 GoogleService-Info.plist 분리 (dev/prod)
- 마이페이지에서 권한 재요청 UX
- 카카오 알림톡 (영구 제외)
