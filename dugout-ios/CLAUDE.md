# CLAUDE.md — dugout-ios (iOS 앱)

> 루트 [`/CLAUDE.md`](../CLAUDE.md) 의 모든 규칙이 적용된다. 이 파일은 iOS 모듈 한정 추가 규칙.

## 빠른 시작

```bash
# 도구 버전 동기화 (mise 사용)
mise install

# 의존성 + 프로젝트 생성
tuist install
tuist generate

# 빌드 (시뮬레이터, generic destination 권장 — 디바이스명 박지 말 것)
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

## Tuist 모듈 구조

`Project.swift`에서 6개 타겟 정의:

```
Dugout (App)
├── DugoutAuthFeature   ← Core/Network + DesignSystem
├── DugoutHomeFeature   ← Core/Network + DesignSystem + Auth + Team
├── DugoutMatchFeature  ← Core/Network + DesignSystem
└── DugoutTeamFeature   ← Core/Network + DesignSystem
```

규칙:
- Feature 모듈은 **Core 모듈에만 의존**. Feature 간 의존은 Home에서만 (집약 화면)
- 새 Feature 추가 시 `Project.swift`에 타겟 + 의존성 명시 필수
- Bundle ID prefix: `com.dugout`
- 모든 타겟: `SWIFT_VERSION=6.0`, `SWIFT_STRICT_CONCURRENCY=complete`

## Feature 내부 디렉토리 (Clean Architecture)

```
Features/{FeatureName}/Sources/
├── Data/
│   ├── DTOs/              # API 응답 매핑 DTO (Codable + Sendable)
│   └── Repositories/      # Repository 구현체
├── Domain/
│   ├── Entities/          # 도메인 모델 (struct + Sendable)
│   └── Repositories/      # Repository 프로토콜 (Sendable)
└── Presentation/
    ├── ViewModels/        # @MainActor + final class + ObservableObject
    └── Views/             # SwiftUI View
```

- DTO → Domain Entity 변환 후 Presentation 전달 (Codable 직접 노출 금지)
- Repository 프로토콜은 Domain에, 구현체는 Data에 (의존성 역전)

## Swift 6 Strict Concurrency 핵심 규칙

| 상황 | 규칙 |
|------|------|
| ViewModel | `@MainActor final class FooViewModel: ObservableObject` |
| 도메인 Entity / DTO | `struct` + `Sendable` |
| Repository 프로토콜 | `protocol Foo: Sendable` |
| Repository 구현체 | `final class` + 모든 프로퍼티 immutable, 또는 `actor` |
| Service / UseCase | `actor` 또는 Sendable final class |
| async 함수 시그니처 | 인자·반환·throws 모두 Sendable |
| 백그라운드 → UI 갱신 | `Task { @MainActor in ... }` 또는 `await MainActor.run { ... }` |

> 위반 패턴 사전 차단 가이드는 [`.claude/skills/swift6-sendable/SKILL.md`](../.claude/skills/swift6-sendable/SKILL.md) 참고.

## 디자인 시스템 (DG 접두어 강제)

`DugoutDesignSystem` 모듈에 정의된 컴포넌트만 사용:

- 모든 컴포넌트는 `DG` 접두어: `DGButton`, `DGCard`, `DGCell`, `DGTextField`, ...
- raw SwiftUI 컴포넌트(`Button`, `TextField`, `List`)는 **DesignSystem 모듈 내부에서만**
- Feature/App 레이어에서는 항상 `DG*` 사용
- 새 컴포넌트 추가 시: `Core/DesignSystem/Sources/Components/DG{Name}.swift`

## 네트워크 (DugoutCoreNetwork + Alamofire)

- `DugoutCoreNetwork`가 Alamofire 래퍼 제공
- 모든 네트워크 호출 인터페이스는 `async throws`
- 401 자동 토큰 리프레시는 `DugoutCoreNetwork` 인터셉터에서 처리
- 호출 측에서 직접 Alamofire 임포트 금지 (Repository 구현체 한정)

## 에러 핸들링 (필수)

- Repository 레이어: `async throws` 또는 `Result<Success, AppError>` 반환
- ViewModel: 에러를 `@Published var errorMessage: String?`(또는 표준 에러 상태)로 노출
- **`try!` / `try?` 무시 금지** (테스트 코드 외)
- 사용자 메시지는 ErrorCode → 한국어 카피 매핑을 통해 표시

## 인증 / 인증서 / Info.plist

- `Info.plist`의 `NSAppTransportSecurity` 예외는 **localhost 한정**
- 운영 도메인에 대한 `NSAllowsArbitraryLoads: true` 절대 금지
- 카카오 등 SDK용 URL Scheme 추가 시 `Project.swift`의 `infoPlist.extendingDefault`에 명시

## Firebase / FCM 셋업 (Phase 3-C 도입)

푸시 알림은 FCM 으로 처리. 카카오 알림톡은 영구 제외.

1. Firebase 콘솔에서 iOS 앱 등록 (Bundle ID: `com.dugout.Dugout`)
2. `GoogleService-Info.plist` 다운로드 → `App/Resources/` 에 저장
3. `tuist install && tuist generate` 후 빌드
4. 실기기에서만 푸시 수신 가능 (시뮬레이터는 iOS 16+ 부터 일부 지원 — `xcrun simctl push` 로 테스트)

`App/Sources/AppDelegate.swift` 는 `GoogleService-Info.plist` 가 번들에 없으면 `FirebaseApp.configure()` 호출을 스킵 (개발 빌드는 plist 없이도 작동). 운영 빌드 전에는 plist 를 반드시 추가하고 가드를 제거 검토.

`App/Sources/Notifications/PushPermissionCoordinator.swift` (actor) 가 토큰 동기화 / 권한 / 로그아웃 정리를 캡슐화. AuthViewModel 은 푸시 로직과 무관.

## 빌드 destination 선택

PR 검증용 빌드는 `generic/platform=iOS Simulator` 권장 (디바이스명 의존성 제거).
특정 디바이스 시뮬레이터 명을 박지 말 것 (Xcode 업그레이드마다 깨짐).

## 테스트

- Unit Test 타겟은 Phase 2 (현재 미구현)
- 추가 시 `Tests/{FeatureName}Tests/Sources/` 구조 권장
- 픽스처에 실제 개인정보(이름·연락처) 형태 절대 금지 — 가상 데이터만
