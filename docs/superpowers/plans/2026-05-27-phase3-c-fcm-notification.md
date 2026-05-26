# Phase 3-C Implementation Plan: FCM 푸시 알림 인프라

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** dugout-api 에 FCM 푸시 알림 인프라를 도입하고, dugout-ios 에서 OAuth 직후 권한 요청 + FCM 토큰 백엔드 동기화 + 라인업 확정 broadcast 1종을 구현한다. 카카오 알림톡 흔적(컬럼/문서/주석)은 영구 제거한다.

**Architecture:** 백엔드는 `domain/notification/` 5폴드 신설 + `global/fcm/` 단일 클라이언트 래퍼. `LineupService.confirmLineup` 이 `LineupConfirmedEvent` 만 발행하고 `NotificationService` 가 `@TransactionalEventListener(AFTER_COMMIT)` 으로 받아 FCM multicast 발송. iOS 는 App 타겟 안에 AppDelegate + actor `PushPermissionCoordinator` 만 추가 (Feature 모듈은 무관). FCM token 은 `users.fcm_token` 컬럼에 저장하며, UNREGISTERED 응답 시 자동 null 처리.

**Tech Stack:** Spring Boot 3.x + Kotlin (백엔드), Firebase Admin SDK 9.x, Swift 6 + SwiftUI, Firebase iOS SDK 11.x (FirebaseMessaging only), Tuist 4.x.

**Reference Spec:** `docs/superpowers/specs/2026-05-27-phase3-c-fcm-notification-design.md`

---

## 0. 사전 준비 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령

```bash
# 백엔드
cd /Users/heetae/Documents/Source/Dugout
make api-test      # compileKotlin compileTestKotlin

# iOS
make ios-build     # warnings 0

# 통합 점검
make seed-check    # api/ai/postgres/redis 4개 UP
```

### 베이스 브랜치

이 plan 은 이미 spec 이 커밋된 `feature/phase3-c-fcm-spec` 브랜치에서 그대로 이어간다. 모든 구현 커밋은 이 브랜치에 누적되며, 모든 task 완료 후 단일 머지로 main 에 반영한다.

```bash
git status                    # feature/phase3-c-fcm-spec, clean
git log --oneline -1          # 031abf3 docs(spec): Phase 3-C FCM ...
```

### Firebase 콘솔 사전 셋업 (CONTROLLER 수동)

다음 두 자산은 사람만 받을 수 있다. Task 시작 전에 준비:

1. **Firebase 콘솔에서 새 프로젝트 생성** ("Dugout") → Spark plan 유지
2. iOS 앱 등록 (Bundle ID `com.dugout.Dugout`) → **`GoogleService-Info.plist`** 다운로드 → `dugout-ios/App/Resources/` 에 저장
3. Project Settings → Service Accounts → Generate new private key → **`firebase-adminsdk.json`** 다운로드 → 로컬 PC 안전한 위치 (git X) 에 저장 → 환경변수 `FCM_CREDENTIALS_PATH` 로 경로 노출
4. Project ID 를 환경변수 `FCM_PROJECT_ID` 로 노출

이 자산 없이도 `FCM_ENABLED=false` 로 로컬 빌드/실행은 가능하다 (stub 동작).

### 재사용 / 신규 / 수정 파일 (전체)

**신규 (백엔드):**
- `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/controller/NotificationController.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/FcmTokenRequest.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/FcmTokenResponse.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/event/LineupConfirmedEvent.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmClient.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmConfig.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmMessage.kt`
- `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmBatchResult.kt`
- `dugout-api/src/main/resources/db/migration/V<N>__drop_kakao_alimtalk_agreed.sql`

**수정 (백엔드):**
- `dugout-api/build.gradle.kts` (Firebase Admin SDK dep)
- `dugout-api/src/main/resources/application.yml` (fcm.* 설정)
- `dugout-api/src/main/kotlin/com/dugout/api/global/error/ErrorCode.kt` (NOTIFICATION_TOKEN_INVALID)
- `dugout-api/src/main/kotlin/com/dugout/api/domain/user/entity/User.kt` (kakaoAlimtalkAgreed 필드 제거)
- `dugout-api/src/main/kotlin/com/dugout/api/domain/lineup/service/LineupService.kt` (이벤트 발행)

**신규 (iOS):**
- `dugout-ios/App/Sources/AppDelegate.swift`
- `dugout-ios/App/Sources/Notifications/PushPermissionCoordinator.swift`
- `dugout-ios/App/Sources/Notifications/PushPermissionPrimingView.swift`
- `dugout-ios/App/Sources/Notifications/NotificationRepository.swift`
- `dugout-ios/App/Resources/GoogleService-Info.plist` (CONTROLLER 가 Firebase 콘솔에서 받아 둠)

**수정 (iOS):**
- `dugout-ios/Tuist/Package.swift` (firebase-ios-sdk SPM)
- `dugout-ios/Project.swift` (App 타겟 dependencies + Info.plist UIBackgroundModes / FirebaseAppDelegateProxyEnabled)
- `dugout-ios/App/Sources/DugoutApp.swift` (UIApplicationDelegateAdaptor)
- `dugout-ios/Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift` (signOut 시 clearToken)
- iOS 로그인 후 진입 분기 화면 — MainTabView 진입 직전 PrimingView 분기 추가 (구체 위치는 Task 단계에서 코드 확인 후 결정)

**수정 (문서):**
- `docs/TDD.md` (section 4 교체, section 2-3 DB 스키마, Feature 단락 갱신)
- `docs/PRD.md` (F9 알림 매트릭스 / 카카오 단락 정리)
- `dugout-api/README.md` (Firebase 셋업 절차)
- `dugout-ios/README.md` (Firebase 셋업 절차)

---

## Milestone 1 — 카카오 흔적 제거 + Backend Notification 도메인 skeleton

### Task 1.1: User entity 에서 kakaoAlimtalkAgreed 제거

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/user/entity/User.kt`

- [ ] **Step 1**: 파일 읽고 현재 컬럼 확인 (`fcmToken`, `kakaoAlimtalkAgreed` 두 개 존재해야 함)

- [ ] **Step 2**: `kakaoAlimtalkAgreed` 필드 제거. 최종 형태:

```kotlin
class User(
    @Column(unique = true)
    var email: String? = null,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Column(length = 20)
    var phone: String? = null,

    @Column(name = "profile_img_url", length = 500)
    var profileImgUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: AuthProvider,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @Column(name = "fcm_token", length = 500)
    var fcmToken: String? = null,
) : BaseEntity() {
    // updateProfile, companion object 는 그대로
}
```

- [ ] **Step 3**: 빌드 확인

```bash
make api-test
```

- [ ] **Step 4**: 다른 곳에서 kakaoAlimtalkAgreed 참조하는 코드 없는지 확인

```bash
grep -rn "kakaoAlimtalkAgreed\|kakao_alimtalk_agreed" dugout-api/src
```

Expected: 0 lines. 있으면 함께 제거.

### Task 1.2: DB 마이그레이션 추가

- [ ] **Step 1**: 기존 마이그레이션 번호 확인

```bash
ls dugout-api/src/main/resources/db/migration/
```

다음 번호를 N 으로 사용 (예: V12 가 마지막이면 V13).

- [ ] **Step 2**: 파일 작성

**File**: `dugout-api/src/main/resources/db/migration/V<N>__drop_kakao_alimtalk_agreed.sql`

```sql
ALTER TABLE users DROP COLUMN kakao_alimtalk_agreed;
```

### Task 1.3: ErrorCode 추가

**File**: `dugout-api/src/main/kotlin/com/dugout/api/global/error/ErrorCode.kt`

- [ ] **Step 1**: 파일 읽고 기존 enum 마지막 항목 확인

- [ ] **Step 2**: 적절한 그룹에 추가 (notification 관련이 없으면 마지막에):

```kotlin
NOTIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "FCM 토큰 형식이 올바르지 않습니다"),
```

### Task 1.4: DTO 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/FcmTokenRequest.kt`

- [ ] **Step 1**:

```kotlin
package com.dugout.api.domain.notification.dto

data class FcmTokenRequest(
    val token: String?,
)
```

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/dto/FcmTokenResponse.kt`

- [ ] **Step 2**:

```kotlin
package com.dugout.api.domain.notification.dto

data class FcmTokenResponse(
    val ok: Boolean,
)
```

### Task 1.5: NotificationService (skeleton) 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt`

- [ ] **Step 1**: PATCH endpoint 만 처리하는 skeleton (이벤트 리스너는 Milestone 3 에서 추가)

```kotlin
package com.dugout.api.domain.notification.service

import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun updateFcmToken(userId: Long, token: String?) {
        val normalized = token?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null && !isValidFcmTokenShape(normalized)) {
            throw BusinessException(ErrorCode.NOTIFICATION_TOKEN_INVALID)
        }
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        user.fcmToken = normalized
    }

    private fun isValidFcmTokenShape(token: String): Boolean {
        // FCM iOS token: 길이 100~300 사이, base64/URL-safe 문자만
        return token.length in 100..300 && token.all {
            it.isLetterOrDigit() || it == '-' || it == '_' || it == ':'
        }
    }
}
```

> `USER_NOT_FOUND` 가 ErrorCode 에 없다면 추가하거나 적절한 기존 코드로 교체. 코드 확인 후 결정.

### Task 1.6: NotificationController 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/controller/NotificationController.kt`

- [ ] **Step 1**:

```kotlin
package com.dugout.api.domain.notification.controller

import com.dugout.api.domain.notification.dto.FcmTokenRequest
import com.dugout.api.domain.notification.dto.FcmTokenResponse
import com.dugout.api.domain.notification.service.NotificationService
import com.dugout.api.global.auth.LoginUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @PatchMapping("/fcm-token")
    fun updateFcmToken(
        @LoginUser userId: Long,
        @RequestBody request: FcmTokenRequest,
    ): ResponseEntity<FcmTokenResponse> {
        notificationService.updateFcmToken(userId, request.token)
        return ResponseEntity.ok(FcmTokenResponse(ok = true))
    }
}
```

> `@LoginUser` 가 프로젝트 표준 어노테이션이 아니면 기존 UserController 가 인증 user 받는 패턴 그대로 사용. 코드 확인 후 맞추기.

### Task 1.7: 빌드 + 커밋

- [ ] **Step 1**:

```bash
make api-test
```

- [ ] **Step 2**:

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/user/entity/User.kt \
        dugout-api/src/main/resources/db/migration/V*__drop_kakao_alimtalk_agreed.sql \
        dugout-api/src/main/kotlin/com/dugout/api/global/error/ErrorCode.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/
git commit -m "feat(api): Notification 도메인 skeleton + 카카오 컬럼 drop (Phase 3-C 1/7)"
```

---

## Milestone 2 — FCM 클라이언트 + Configuration

### Task 2.1: Firebase Admin SDK 의존성 추가

**File**: `dugout-api/build.gradle.kts`

- [ ] **Step 1**: `dependencies` 블록에 추가 (적절한 위치, 다른 google/firebase 의존성 근처 또는 마지막):

```kotlin
implementation("com.google.firebase:firebase-admin:9.4.1")
```

- [ ] **Step 2**: gradle sync 확인

```bash
cd dugout-api && ./gradlew dependencies --configuration runtimeClasspath --quiet | grep firebase-admin
```

Expected: `com.google.firebase:firebase-admin:9.4.1` 출력.

### Task 2.2: application.yml 에 fcm.* 설정 추가

**File**: `dugout-api/src/main/resources/application.yml`

- [ ] **Step 1**: 적절한 위치 (다른 외부 서비스 설정 근처) 에 추가:

```yaml
fcm:
  enabled: ${FCM_ENABLED:false}
  credentials-path: ${FCM_CREDENTIALS_PATH:}
  project-id: ${FCM_PROJECT_ID:}
```

### Task 2.3: FcmMessage / FcmBatchResult 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmMessage.kt`

- [ ] **Step 1**:

```kotlin
package com.dugout.api.global.fcm

data class FcmMessage(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val sound: String = "default",
    val badge: Int? = 1,
)
```

**File**: `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmBatchResult.kt`

- [ ] **Step 2**:

```kotlin
package com.dugout.api.global.fcm

data class FcmBatchResult(
    val successCount: Int,
    val failureCount: Int,
    val invalidTokens: List<String>,    // UNREGISTERED / INVALID_ARGUMENT
) {
    companion object {
        fun empty() = FcmBatchResult(0, 0, emptyList())
        fun stub() = FcmBatchResult(0, 0, emptyList())
    }
}
```

### Task 2.4: FcmConfig 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmConfig.kt`

- [ ] **Step 1**:

```kotlin
package com.dugout.api.global.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FcmConfig(
    @Value("\${fcm.enabled}") private val enabled: Boolean,
    @Value("\${fcm.credentials-path}") private val credentialsPath: String,
    @Value("\${fcm.project-id}") private val projectId: String,
) {
    private val log = LoggerFactory.getLogger(FcmConfig::class.java)

    @Bean
    fun firebaseMessaging(): FirebaseMessaging? {
        if (!enabled) {
            log.info("FCM disabled (fcm.enabled=false) — stub client active")
            return null
        }
        require(credentialsPath.isNotBlank()) { "fcm.credentials-path must be set when fcm.enabled=true" }
        require(projectId.isNotBlank()) { "fcm.project-id must be set when fcm.enabled=true" }

        val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsPath))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build()
        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
        log.info("FCM enabled (project=$projectId)")
        return FirebaseMessaging.getInstance(app)
    }
}
```

### Task 2.5: FcmClient 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/global/fcm/FcmClient.kt`

- [ ] **Step 1**:

```kotlin
package com.dugout.api.global.fcm

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FcmClient(
    private val firebaseMessaging: FirebaseMessaging?,
) {
    private val log = LoggerFactory.getLogger(FcmClient::class.java)

    fun sendToTokens(tokens: List<String>, message: FcmMessage): FcmBatchResult {
        if (tokens.isEmpty()) return FcmBatchResult.empty()
        val messaging = firebaseMessaging ?: run {
            log.debug("FCM stub: would send '${message.title}' to ${tokens.size} tokens")
            return FcmBatchResult.stub()
        }

        val multicast = MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(message.title)
                    .setBody(message.body)
                    .build()
            )
            .putAllData(message.data)
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound(message.sound)
                            .setBadge(message.badge ?: 0)
                            .build()
                    )
                    .build()
            )
            .addAllTokens(tokens)
            .build()

        return try {
            val response = messaging.sendEachForMulticast(multicast)
            val invalid = mutableListOf<String>()
            response.responses.forEachIndexed { index, resp ->
                if (!resp.isSuccessful) {
                    val code = resp.exception?.messagingErrorCode?.name
                    log.warn("FCM send failed for token ${tokens[index].take(8)}... : $code")
                    if (code == "UNREGISTERED" || code == "INVALID_ARGUMENT") {
                        invalid += tokens[index]
                    }
                }
            }
            FcmBatchResult(
                successCount = response.successCount,
                failureCount = response.failureCount,
                invalidTokens = invalid,
            )
        } catch (e: Exception) {
            log.error("FCM multicast send error: ${e.message}", e)
            FcmBatchResult.empty()
        }
    }
}
```

### Task 2.6: 빌드 + 커밋

- [ ] **Step 1**:

```bash
make api-test
```

- [ ] **Step 2**:

```bash
git add dugout-api/build.gradle.kts \
        dugout-api/src/main/resources/application.yml \
        dugout-api/src/main/kotlin/com/dugout/api/global/fcm/
git commit -m "feat(api): FCM client + Firebase Admin SDK 도입 (Phase 3-C 2/7)"
```

---

## Milestone 3 — LineupConfirmedEvent + TransactionalEventListener

### Task 3.1: LineupConfirmedEvent 작성

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/event/LineupConfirmedEvent.kt`

- [ ] **Step 1**:

```kotlin
package com.dugout.api.domain.notification.event

data class LineupConfirmedEvent(
    val lineupId: Long,
    val matchId: Long,
    val teamId: Long,
    val confirmedBy: Long,
)
```

> 위치 결정: `event/` 는 Notification 도메인 안에 둔다. 이유: 이벤트의 consumer 가 Notification 도메인이고, Lineup 은 publish 만 한다. consumer-side 패키지 선택이 의존 역전의 명시.

### Task 3.2: LineupService.confirmLineup 에 이벤트 발행 추가

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/lineup/service/LineupService.kt`

- [ ] **Step 1**: 파일 읽고 `confirmLineup` 메서드 시그니처/현재 구조 파악

- [ ] **Step 2**: 생성자에 `ApplicationEventPublisher` 주입 추가 (없으면). import:

```kotlin
import com.dugout.api.domain.notification.event.LineupConfirmedEvent
import org.springframework.context.ApplicationEventPublisher
```

- [ ] **Step 3**: `confirmLineup` 메서드의 lineup 상태 변경 후 `return` 직전에 이벤트 발행 추가:

```kotlin
applicationEventPublisher.publishEvent(
    LineupConfirmedEvent(
        lineupId = lineup.id,
        matchId = lineup.matchId,
        teamId = match.teamId,
        confirmedBy = userId,
    )
)
```

> `match` 객체에서 teamId 접근. 기존 confirmLineup 구현에 match 가 없으면 matchRepository 로 조회. 코드 확인 후 결정.

### Task 3.3: NotificationService 에 이벤트 리스너 추가

**File**: `dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt`

- [ ] **Step 1**: imports 추가:

```kotlin
import com.dugout.api.domain.notification.event.LineupConfirmedEvent
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.global.fcm.FcmClient
import com.dugout.api.global.fcm.FcmMessage
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
```

- [ ] **Step 2**: 생성자에 의존성 추가:

```kotlin
@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val fcmClient: FcmClient,
) {
```

- [ ] **Step 3**: 클래스 안에 리스너 메서드 추가:

```kotlin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onLineupConfirmed(event: LineupConfirmedEvent) {
        val members = teamMemberRepository.findByTeamId(event.teamId)
        val targetUsers = members
            .map { it.user }
            .filter { it.id != event.confirmedBy }
        val tokens = targetUsers.mapNotNull { it.fcmToken }
        if (tokens.isEmpty()) return

        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = buildLineupConfirmedMessage(match, event.lineupId)
        val result = fcmClient.sendToTokens(tokens, payload)
        cleanUpInvalidTokens(targetUsers, result.invalidTokens)
    }

    private fun buildLineupConfirmedMessage(match: Match, lineupId: Long): FcmMessage {
        val date = match.matchDate
        val dateText = "${date.monthValue}월 ${date.dayOfMonth}일 (${
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        })"
        val parts = buildList {
            add(dateText)
            match.groundName?.let { add(it) }
            match.opponentName?.let { add("vs $it") }
        }
        return FcmMessage(
            title = "라인업이 확정됐어요",
            body = parts.joinToString(" · "),
            data = mapOf(
                "type" to "LINEUP_CONFIRMED",
                "matchId" to match.id.toString(),
                "teamId" to match.teamId.toString(),
                "lineupId" to lineupId.toString(),
            ),
        )
    }

    @Transactional
    fun cleanUpInvalidTokens(users: List<User>, invalidTokens: List<String>) {
        if (invalidTokens.isEmpty()) return
        users.filter { it.fcmToken in invalidTokens }.forEach { it.fcmToken = null }
    }
```

- [ ] **Step 4**: `import` 추가: `com.dugout.api.domain.match.entity.Match`, `com.dugout.api.domain.user.entity.User`.

### Task 3.4: 빌드 + 커밋

- [ ] **Step 1**:

```bash
make api-test
```

- [ ] **Step 2**:

```bash
git add dugout-api/src/main/kotlin/com/dugout/api/domain/notification/event/ \
        dugout-api/src/main/kotlin/com/dugout/api/domain/notification/service/NotificationService.kt \
        dugout-api/src/main/kotlin/com/dugout/api/domain/lineup/service/LineupService.kt
git commit -m "feat(api): 라인업 확정 broadcast — Event + AFTER_COMMIT Listener (Phase 3-C 3/7)"
```

---

## Milestone 4 — iOS: Firebase SDK + AppDelegate

### Task 4.1: Tuist Package.swift 에 Firebase SDK 추가

**File**: `dugout-ios/Tuist/Package.swift`

- [ ] **Step 1**: 파일 읽고 현재 dependencies 확인

- [ ] **Step 2**: dependencies 에 추가:

```swift
.package(url: "https://github.com/firebase/firebase-ios-sdk", from: "11.0.0"),
```

- [ ] **Step 3**: Tuist resolve

```bash
cd dugout-ios && tuist install
```

### Task 4.2: Project.swift App 타겟에 FirebaseMessaging link + Info.plist 보완

**File**: `dugout-ios/Project.swift`

- [ ] **Step 1**: App 타겟의 `dependencies` 배열에 추가 (기존 Feature 모듈들 다음):

```swift
.external(name: "FirebaseMessaging"),
```

- [ ] **Step 2**: App 타겟의 `infoPlist.extendingDefault` dict 에 추가:

```swift
"UIBackgroundModes": ["remote-notification"],
"FirebaseAppDelegateProxyEnabled": false,
```

> `FirebaseAppDelegateProxyEnabled: false` — Firebase 의 자동 swizzling 끄고 우리 AppDelegate 가 명시적으로 처리. Swift 6 concurrency 와 호환성 좋음.

- [ ] **Step 3**: Tuist 재생성

```bash
cd dugout-ios && tuist generate
```

### Task 4.3: GoogleService-Info.plist 배치 (CONTROLLER)

- [ ] **Step 1**: Firebase 콘솔에서 받은 `GoogleService-Info.plist` 를 `dugout-ios/App/Resources/` 에 저장

- [ ] **Step 2**: 파일 존재 확인

```bash
ls dugout-ios/App/Resources/GoogleService-Info.plist
```

> 이 파일이 없으면 빌드 시 `FirebaseApp.configure()` 가 런타임 크래시 발생. Firebase 콘솔 셋업 완료 전이면 임시로 `App/Resources/` 에 더미 plist (Bundle ID 만 맞춰 둠) 를 두거나, configure 호출 자체를 임시로 건너뛰는 가드 추가 (운영 전 제거).

### Task 4.4: AppDelegate 작성

**File**: `dugout-ios/App/Sources/AppDelegate.swift`

- [ ] **Step 1**:

```swift
//
//  AppDelegate.swift
//  Dugout
//

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

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs register failed: \(error.localizedDescription)")
    }
}
```

### Task 4.5: DugoutApp 에 AppDelegate adapter 연결

**File**: `dugout-ios/App/Sources/DugoutApp.swift`

- [ ] **Step 1**: 파일 읽고 현재 `@main` 구조 확인

- [ ] **Step 2**: `@main struct DugoutApp: App` 안에 추가 (기존 프로퍼티들 위 또는 init 직전):

```swift
@UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
```

> 이 시점에서 `PushPermissionCoordinator` 아직 없음 — Task 5.x 에서 추가. 빌드는 5.x 까지 마친 후 한 번에 (이 단계는 잠시 깨진 상태).

### Task 4.6: 빌드 보류 (다음 milestone 후 일괄 빌드)

- [ ] **Step 1**: 이 단계는 PushPermissionCoordinator 가 없어서 빌드 실패 예상. Milestone 5 끝에 한 번에 빌드 + 커밋.

---

## Milestone 5 — iOS: Notification 인프라 (Coordinator + Repository + PrimingView)

### Task 5.1: NotificationRepository 프로토콜 + 구현

**File**: `dugout-ios/App/Sources/Notifications/NotificationRepository.swift`

- [ ] **Step 1**:

```swift
//
//  NotificationRepository.swift
//  Dugout
//
//  FCM 토큰 등록/갱신/해제 백엔드 호출.
//

import Foundation
import DugoutCoreNetwork

public protocol NotificationRepository: Sendable {
    func patchFcmToken(_ token: String?) async throws
}

final class NotificationRepositoryImpl: NotificationRepository {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func patchFcmToken(_ token: String?) async throws {
        struct Body: Encodable { let token: String? }
        struct Response: Decodable { let ok: Bool }
        let endpoint = APIEndpoint(
            path: "/api/v1/users/me/fcm-token",
            method: .patch,
            body: Body(token: token)
        )
        let _: Response = try await client.request(endpoint)
    }
}
```

> `APIClient.shared` / `APIEndpoint` 의 정확한 시그니처는 `DugoutCoreNetwork` 의 기존 사용처(`LineupRepositoryImpl` 등) 와 맞춰서 조정. 코드 확인 후 alignment.

### Task 5.2: PushPermissionCoordinator (actor) 작성

**File**: `dugout-ios/App/Sources/Notifications/PushPermissionCoordinator.swift`

- [ ] **Step 1**:

```swift
//
//  PushPermissionCoordinator.swift
//  Dugout
//
//  FCM 토큰 동기화 + 시스템 권한 요청을 캡슐화하는 actor.
//  AppDelegate 가 MessagingDelegate / UNUserNotificationCenterDelegate 로 등록.
//

import FirebaseMessaging
import Foundation
import UIKit
import UserNotifications

actor PushPermissionCoordinator: NSObject {
    static let shared = PushPermissionCoordinator()

    private let repository: any NotificationRepository
    private var lastSyncedToken: String?

    override init() {
        self.repository = NotificationRepositoryImpl()
        super.init()
    }

    /// 시스템 권한 요청. 허용 시 APNs 등록까지 트리거.
    /// - Returns: 사용자가 허용했으면 true.
    func requestAuthorization() async -> Bool {
        let granted = (try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        if granted {
            await MainActor.run {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
        return granted
    }

    /// 시스템에 이미 권한이 있으면 APNs 재등록만.
    func registerIfAuthorized() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        if settings.authorizationStatus == .authorized {
            await MainActor.run {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    /// FCM 콜백에서 받은 새 토큰을 백엔드에 동기화. 동일하면 skip.
    func handleNewToken(_ token: String?) async {
        let normalized = token?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalized != lastSyncedToken else { return }
        do {
            try await repository.patchFcmToken(normalized)
            lastSyncedToken = normalized
        } catch {
            print("FCM token sync failed: \(error.localizedDescription)")
        }
    }

    /// 로그아웃 시 백엔드 토큰 null 처리 + 로컬 토큰 폐기.
    func clearToken() async {
        do {
            try await repository.patchFcmToken(nil)
            lastSyncedToken = nil
        } catch {
            print("FCM token clear failed: \(error.localizedDescription)")
        }
        await MainActor.run {
            Messaging.messaging().deleteToken { _ in }
        }
    }
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

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        // S 범위: deeplink 처리 없음. 앱이 열리기만 함.
    }
}
```

### Task 5.3: PushPermissionPrimingView 작성

**File**: `dugout-ios/App/Sources/Notifications/PushPermissionPrimingView.swift`

- [ ] **Step 1**:

```swift
//
//  PushPermissionPrimingView.swift
//  Dugout
//
//  OAuth 직후 시스템 권한 dialog 띄우기 전의 priming UI.
//

import SwiftUI
import DugoutDesignSystem

struct PushPermissionPrimingView: View {
    let onAllow: () async -> Void
    let onSkip: () -> Void

    var body: some View {
        VStack(spacing: DGSpacing.xl) {
            Spacer()
            Image(systemName: "bell.badge.fill")
                .font(.system(size: 72))
                .foregroundStyle(DGColor.p500)
            Text("알림을 받아볼까요?")
                .font(DGFont.pretendard(.bold, size: 24))
                .foregroundStyle(DGColor.c900)
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                bullet(emoji: "⚾", text: "경기 일정 등록")
                bullet(emoji: "📋", text: "라인업 확정")
                bullet(emoji: "⏰", text: "출석 응답 리마인드")
            }
            .padding(.horizontal, DGSpacing.xl)
            Spacer()
            VStack(spacing: DGSpacing.md) {
                DGButton("알림 허용", style: .primary) {
                    Task { await onAllow() }
                }
                DGButton("나중에", style: .tertiary, action: onSkip)
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.bottom, DGSpacing.xxl)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.c0)
    }

    private func bullet(emoji: String, text: String) -> some View {
        HStack(spacing: DGSpacing.md) {
            Text(emoji).font(.system(size: 22))
            Text(text)
                .font(DGFont.pretendard(.regular, size: 18))
                .foregroundStyle(DGColor.c700)
        }
    }
}
```

> `DGButton` 시그니처는 4-B 의 `LineupView` 사용 패턴 (`isLoading`, `isEnabled`, `style`) 참고. `.primary`, `.tertiary` 존재 가정. 코드 확인 후 alignment.

### Task 5.4: 빌드 + 커밋

- [ ] **Step 1**:

```bash
make ios-build
```

> 이 시점에서 `DugoutApp` 안 어디서도 PrimingView 를 띄우지 않으므로 컴파일은 통과. PushPermissionCoordinator.shared 가 AppDelegate 에서 참조되는 것만 link.

- [ ] **Step 2**:

```bash
git add dugout-ios/Tuist/Package.swift \
        dugout-ios/Project.swift \
        dugout-ios/App/Sources/AppDelegate.swift \
        dugout-ios/App/Sources/DugoutApp.swift \
        dugout-ios/App/Sources/Notifications/
git commit -m "feat(ios): Firebase SDK + AppDelegate + PushPermissionCoordinator (Phase 3-C 4/7)"
```

---

## Milestone 6 — iOS: 권한 분기 흐름 통합 + 로그아웃 정리

### Task 6.1: OAuth 직후 진입 분기에 PrimingView 연결

- [ ] **Step 1**: OAuth 콜백 후 MainTabView 로 진입하는 코드 위치 파악

```bash
grep -rn "MainTabView\|AuthViewModel\|signInSuccess" \
  dugout-ios/App/Sources dugout-ios/Features/Auth/Sources
```

- [ ] **Step 2**: 진입 분기 위치 (보통 `DugoutApp.swift` 또는 `App/Sources/MainTabView.swift` 의 상위 컨테이너) 에서 권한 상태에 따라 PrimingView vs MainTabView 분기 추가

예시 구조 (실제 구조에 맞춰 조정):

```swift
struct RootView: View {
    @State private var didPromptPush = false
    @State private var primingShown = false

    var body: some View {
        if isAuthenticated && !primingShown && shouldPromptPush {
            PushPermissionPrimingView(
                onAllow: {
                    _ = await PushPermissionCoordinator.shared.requestAuthorization()
                    primingShown = true
                },
                onSkip: { primingShown = true }
            )
            .task {
                let settings = await UNUserNotificationCenter.current().notificationSettings()
                shouldPromptPush = (settings.authorizationStatus == .notDetermined)
            }
        } else {
            MainTabView()
                .task {
                    if isAuthenticated {
                        await PushPermissionCoordinator.shared.registerIfAuthorized()
                    }
                }
        }
    }
}
```

> 정확한 분기 위치/state 관리는 기존 인증 흐름의 source-of-truth 에 맞춰 조정. 핵심은 OAuth 성공 시점에 `notDetermined` 면 PrimingView, 아니면 바로 MainTabView + `registerIfAuthorized()`.

### Task 6.2: 로그아웃 시 FCM 토큰 정리 (App 타겟 watcher)

**Design 결정**: `PushPermissionCoordinator` 는 App 타겟에 위치하며 Feature 모듈에서 import 불가 (의존 방향 역행). 따라서 AuthViewModel.swift 는 건드리지 않고, App 타겟의 root view 가 인증 상태 변화를 관찰하여 `clearToken()` 호출.

**File**: Task 6.1 에서 수정한 root view (보통 `DugoutApp.swift` 또는 `App/Sources/MainTabView.swift` 의 상위 컨테이너)

- [ ] **Step 1**: 인증 상태 source-of-truth 식별

```bash
grep -rn "isAuthenticated\|isLoggedIn\|@Published.*session" \
  dugout-ios/Features/Auth/Sources dugout-ios/App/Sources
```

- [ ] **Step 2**: Task 6.1 에서 분기에 사용한 동일 인증 state 를 관찰하여 logout 감지

root view 의 body 에 `onChange` 추가:

```swift
.onChange(of: isAuthenticated) { _, newValue in
    if !newValue {
        Task { await PushPermissionCoordinator.shared.clearToken() }
    }
}
```

> 인증 상태가 `true → false` 로 전환되는 모든 경로 (사용자 로그아웃 / 토큰 만료 / 강제 로그아웃) 에서 일관되게 동작한다. AuthViewModel 측 변경 0.

### Task 6.3: 빌드 + 커밋

- [ ] **Step 1**:

```bash
make ios-build
```

- [ ] **Step 2**: 변경 파일 확인

```bash
git status
```

- [ ] **Step 3**: 변경된 분기 진입 화면 + (있다면) AuthViewModel 일괄 커밋:

```bash
git add <touched-files>
git commit -m "feat(ios): OAuth 직후 푸시 권한 priming + 로그아웃 시 토큰 정리 (Phase 3-C 5/7)"
```

---

## Milestone 7 — 문서 갱신 + 통합 검증

### Task 7.1: docs/TDD.md section 4 교체

**File**: `docs/TDD.md`

- [ ] **Step 1**: section 4-1 (953-988) 전체 교체

기존:
```markdown
### 4-1. 카카오 알림톡 연동

```kotlin
// 카카오 비즈메시지 알림톡 발송
class KakaoAlimtalkClient(...) { ... }
```
```

다음으로 교체:

```markdown
### 4-1. FCM 통합 (Firebase Cloud Messaging)

Phase 3-C 에서 도입. 카카오 알림톡은 발송당 과금 비용 + 비즈니스 채널 심사 부담으로 영구 제외하고 FCM 만 사용한다. 앱 미설치자 도달은 비범위.

**아키텍처:**
- `global/fcm/FcmClient` — Firebase Admin SDK 단일 래퍼. `sendToTokens(tokens, message)` 시그니처. `fcm.enabled=false` 면 stub 동작.
- `domain/notification/` — controller (PATCH /api/v1/users/me/fcm-token) / service (다른 도메인이 호출하는 진입점) / dto / event
- `users.fcm_token` 컬럼 직접 사용. 별도 토큰 테이블 없음 (디바이스별 분리는 후속 Phase).

**Payload 구조:**
- `notification` 블록: title + body (iOS 시스템이 자동 표시)
- `data` 블록: `type` / `matchId` / `teamId` / `lineupId` (deeplink 후속 Phase 용)
- `apns.payload.aps`: sound + badge

**실패 처리:**
- `UNREGISTERED` / `INVALID_ARGUMENT` → `users.fcm_token = null` 자동 정리
- 다른 FCM 오류 → 로그만, 비즈니스 트랜잭션에 영향 X

**설정 (application.yml):**
- `fcm.enabled` — 로컬 false 기본
- `fcm.credentials-path` — service account JSON 경로 (env)
- `fcm.project-id` — Firebase 프로젝트 ID
```

- [ ] **Step 2**: section 4-2 (990-1012) 전체 교체

기존 카카오 알림톡 발송 플로우 문자열 → 다음:

```markdown
### 4-2. 이벤트 기반 발송 플로우

Phase 3-C 범위는 **라인업 확정 broadcast 1종**. 다른 알림(매치 등록, 출석 리마인드 cron, 알림 설정 UI)은 후속 Phase.

```
LineupController.confirmLineup
  └─ LineupService.confirmLineup() [@Transactional]
       ├─ Lineup.isConfirmed = true
       └─ publishEvent(LineupConfirmedEvent)
                            ↓ AFTER_COMMIT
NotificationService.onLineupConfirmed [@TransactionalEventListener]
  ├─ teamMemberRepository.findByTeamId
  ├─ tokens = members.filter(주장 제외).mapNotNull(fcmToken)
  ├─ fcmClient.sendToTokens(tokens, payload)
  └─ invalidTokens → users.fcm_token = null
```

이 패턴이 도메인 의존 역전을 보장한다. `LineupService` 는 `NotificationService` 를 import 하지 않으며, 이벤트만 발행.
```

- [ ] **Step 3**: section 2-3 users 테이블 스키마에서 `kakao_alimtalk_agreed` 컬럼 제거

```bash
grep -n "kakao_alimtalk_agreed" docs/TDD.md
```

해당 라인이 있으면 삭제.

- [ ] **Step 4**: DugoutMatchFeature / DugoutLineupFeature 단락 (TDD 1133, 1137 부근) 의 "카카오 알림톡·푸시 알림은 후속 Phase 예정" 문구 갱신

기존:
> ... 카카오 알림톡·푸시 알림은 후속 Phase 예정이다.

다음으로 교체:
> ... 푸시 알림은 Phase 3-C 에서 FCM 으로 도입. 카카오 알림톡은 비용/운영 부담으로 영구 스코프 외. 매치 등록 broadcast / 출석 리마인드 cron / 알림 설정 UI 는 후속 Phase.

### Task 7.2: docs/PRD.md F9 갱신

**File**: `docs/PRD.md`

- [ ] **Step 1**: F9-1 (390-393) 알림 채널에서 카카오 알림톡 행 제거. 최종 형태:

```markdown
- F9-1: 알림 채널
  - 앱 푸시 알림 (FCM, Phase 3-C 도입)
  - 이메일 (선택, 후속 Phase)
```

- [ ] **Step 2**: F9-2 알림 매트릭스 (396-406) 의 "알림톡" 열 제거. "푸시" 열만 남김.

```markdown
| 알림 유형 | 발송 시점 | 푸시 |
|-----------|-----------|------|
| 새 경기 일정 | 일정 등록 즉시 | ✅ |
| 출석 리마인드 | 경기 48h, 24h 전 | ✅ |
| 라인업 확정 | 주장 확정 시 | ✅ |
| ... | ... | ... |
```

- [ ] **Step 3**: "카카오 알림톡 전략" 단락 (413-414) 완전 삭제. 다음으로 대체:

```markdown
**FCM 우선 전략:**
> Phase 3-C 에서 FCM 으로 푸시 알림을 도입. 카카오 알림톡은 발송당 과금 비용 + 비즈니스 채널 심사 부담으로 영구 제외. 결과적으로 앱 미설치자 도달은 포기하지만, 앱 설치 사용자의 알림 경험에 집중하여 무료/즉시/Android 호환의 이점을 확보. 사용자 전환은 앱 설치 자체를 유도하는 마케팅/온보딩 흐름으로 대체.
```

### Task 7.3: README.md Firebase 셋업 절차 추가

**File**: `dugout-api/README.md`

- [ ] **Step 1**: "환경변수" 또는 "셋업" 단락에 추가:

```markdown
### FCM (선택 — 푸시 알림 발송용)

로컬 개발에서는 `FCM_ENABLED=false` (기본) 로 stub 동작. 실제 발송이 필요한 경우:

1. Firebase 콘솔 → Project Settings → Service Accounts → "Generate new private key"
2. 다운로드된 JSON 을 git 에 올리지 말고 안전한 위치에 저장
3. 환경변수 설정:
   - `FCM_ENABLED=true`
   - `FCM_CREDENTIALS_PATH=/absolute/path/to/firebase-adminsdk.json`
   - `FCM_PROJECT_ID=your-firebase-project-id`
4. 백엔드 재시작 → 로그에서 "FCM enabled (project=...)" 확인
```

**File**: `dugout-ios/README.md`

- [ ] **Step 2**:

```markdown
### Firebase 셋업 (푸시 알림용)

1. Firebase 콘솔에서 iOS 앱 등록 (Bundle ID: `com.dugout.Dugout`)
2. `GoogleService-Info.plist` 다운로드 → `App/Resources/` 에 저장
3. `tuist install && tuist generate` 후 빌드
4. 실기기에서만 푸시 수신 가능 (시뮬레이터는 iOS 16+ 부터 일부 지원 — `xcrun simctl push` 로 테스트)
```

### Task 7.4: 문서 커밋

- [ ] **Step 1**:

```bash
git add docs/TDD.md docs/PRD.md \
        dugout-api/README.md dugout-ios/README.md
git commit -m "docs: TDD section 4 / PRD F9 — FCM 도입 + 카카오 알림톡 영구 제외 반영 (Phase 3-C 6/7)"
```

### Task 7.5: 통합 빌드 + PII 점검

- [ ] **Step 1**:

```bash
make api-test
make ios-build
```

둘 다 통과.

- [ ] **Step 2**: PII 가드 점검

```bash
grep -rEn "(print|log\.|os_log).*(fcmToken|token)" \
  dugout-api/src/main/kotlin dugout-ios/App
```

Expected: raw token 출력 0 줄 (prefix 8자 마스킹 패턴만 있음).

- [ ] **Step 3**: 카카오 흔적 잔존 확인

```bash
grep -rEn "kakao.*alimtalk|kakaoAlimtalk|alimtalk_agreed" \
  dugout-api/src dugout-ios docs
```

Expected: spec 자체 (`docs/superpowers/specs/2026-05-27-...`) 안의 "카카오 알림톡 제외" 설명 단락 외 0 줄.

### Task 7.6: 수동 검증 시나리오 (CONTROLLER)

> CONTROLLER 가 수동으로 진행. 자동 task 아님.

**준비:**
- Firebase 콘솔 셋업 완료 + `GoogleService-Info.plist`, service account JSON 배치
- `FCM_ENABLED=true` 로 백엔드 실행
- 실기기 3대 (주장 1 + 멤버 2) — 시뮬레이터는 푸시 수신 제약

**시나리오:**

1. 3 디바이스 각각 OAuth 로그인 → priming view 표시 → "알림 허용" → 시스템 dialog 허용 → 백엔드 로그에서 `PATCH /fcm-token` 3회 확인
2. 주장이 라인업 추천 → 저장 → "확정" 탭 → 200 OK
3. 멤버 디바이스 2대에서 배너 수신 ("라인업이 확정됐어요", body 에 날짜/구장/상대팀)
4. 한 멤버 디바이스 앱 삭제 → 주장이 다시 확정 시도 (`LINEUP_ALREADY_CONFIRMED` 가 막으면 다른 매치로) → 백엔드 로그에 `UNREGISTERED` 응답 → DB 의 해당 user.fcm_token = null 확인
5. `FCM_ENABLED=false` 재기동 → 라인업 확정 시 백엔드 로그 "FCM stub: would send 'L...' to N tokens" → iOS 알림 안 옴 → 비즈니스 응답은 정상
6. 한 디바이스 로그아웃 → 백엔드 로그에 `PATCH /fcm-token { token: null }` 1회 → DB user.fcm_token = null 확인

**문제 발견 시:** 새 task 추가하지 말고 plan 중단, controller 에 보고.

### Task 7.7: 머지 + push (CONTROLLER)

> CONTROLLER 수동 진행. 자동 task 아님.

```bash
git checkout main
git merge --no-ff feature/phase3-c-fcm-spec -m "Merge branch 'feature/phase3-c-fcm-spec'

Phase 3-C 완료 (FCM 푸시 알림 인프라):
- domain/notification 5폴드 신설 + global/fcm Firebase Admin SDK 래퍼
- LineupConfirmedEvent + @TransactionalEventListener(AFTER_COMMIT)
- iOS AppDelegate + PushPermissionCoordinator actor + PrimingView
- OAuth 직후 권한 분기 + 로그아웃 시 토큰 정리
- 카카오 알림톡 영구 제외 (DB drop column + TDD/PRD 정리)"

git branch -d feature/phase3-c-fcm-spec
git push origin main
```

---

## 검증 체크리스트

- [ ] `make api-test` 성공
- [ ] `make ios-build` 성공 (warnings 0)
- [ ] 카카오 흔적 0 (spec 안 설명문 외)
- [ ] PII 로그 노출 0 (raw FCM token 출력 X)
- [ ] DB 마이그레이션 (kakao_alimtalk_agreed drop) 정상 적용
- [ ] 새 ErrorCode `NOTIFICATION_TOKEN_INVALID` 1개 추가
- [ ] `application.yml` fcm.* 3개 키 추가
- [ ] iOS Project.swift 에 FirebaseMessaging link + UIBackgroundModes + FirebaseAppDelegateProxyEnabled
- [ ] 수동 시나리오 1~6 통과
- [ ] `docs/TDD.md` section 4 / 2-3 / Feature 단락 갱신
- [ ] `docs/PRD.md` F9 카카오 제외 반영
- [ ] 양쪽 README 에 Firebase 셋업 절차 추가
- [ ] 7 commits + main 머지 + push 완료

---

## 후속 Phase 후보 (본 plan 비범위)

다음 항목은 각각 별도 spec/plan 으로:

- 매치 등록 broadcast (`MatchService.createMatch` → MatchCreatedEvent)
- 출석 응답 변경 broadcast
- 출석 리마인드 cron 스케줄러 (`@Scheduled`, 48h/24h 전)
- 알림 설정 화면 (유형별 on/off, DnD 시간)
- 알림 탭 → 화면 deeplink (`NotificationRouter`)
- `NotificationLog` 감사 엔티티
- topic 기반 발송 (token loop → topic subscribe)
- 환경별 GoogleService-Info.plist 분리 (dev/prod)
- 마이페이지에서 권한 재요청 UX
- (영구 제외) 카카오 알림톡
