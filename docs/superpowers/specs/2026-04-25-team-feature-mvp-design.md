# Team Feature MVP — Design Spec

- 작성일: 2026-04-25
- 대상: iOS 앱 (`dugout-ios`) + Backend (`dugout-api`) 연동
- 상태: Approved (사용자 승인 완료, 구현 계획 작성 단계)

## 1. 배경

`dugout-api` 백엔드는 팀 도메인 핵심 API(생성·조회·가입·멤버 목록·초대 코드)가 90% 이상 구현된 상태다. iOS 측은 Domain/Data 계층(Entity, Repository protocol·구현체, DTO)이 거의 갖춰졌으나 **Presentation 계층(View/ViewModel)이 비어 있다**. 또한 메인 탭 셸과 인증 통합 진입 흐름이 아직 만들어지지 않았다.

이 사이클의 목표는 iOS에서 **팀 생성·가입·상세 조회까지 동작하는 end-to-end 사용자 흐름**을 완성하고, 동시에 향후 다른 feature(일정·출석·매칭 등)가 붙을 **메인 탭 셸 + 인증 통합 패턴**을 확립하는 것이다.

## 2. 작업 범위

### 포함 (In Scope)

- iOS 메인 탭 셸 (Splash → MainTabView, 2탭: 홈 / 마이페이지)
- 홈 탭: 내 팀 목록 + 빈 상태 처리 + 팀 만들기/가입 진입
- 팀 생성 화면 (sheet) — 이름·지역·부수·활동 요일·라인업 모드 입력
- 팀 가입 화면 (sheet) — 초대 코드 1단계 즉시 가입
- 팀 상세 화면 (push) — 팀 기본 정보 + 멤버 목록 + 초대 코드(CAPTAIN 한정)
- 마이페이지 탭 — 사용자 정보 표시 + 로그아웃
- Deferred Auth 패턴 — 비로그인 상태에서도 메인 진입, 인증 필요 액션 시 LoginSheet 트리거 후 자동 이어가기
- iOS Domain/Data 보강 — `MyTeam` Entity, `MyTeamDTO`, `TeamRepository.fetchMyTeams()` 추가
- 디자인: 기본 SwiftUI (DG 디자인 시스템 도입은 후속)

### 미포함 (Out of Scope)

- 팀 정보 수정 (`PUT /teams/{id}`) — 다음 사이클
- 멤버 추방·역할 변경 (`PUT/DELETE /members/{memberId}`) — 다음 사이클
- DG 디자인 시스템(컴포넌트 래퍼, 색·간격·폰트 토큰) — UI/UX 결정 후 별도 사이클
- 팀 가입 미리보기(2단계 가입 플로우) — 백엔드 API 추가 동반, UX 개선 사이클로 분리
- 백엔드 `GET /api/v1/users/me` — 현재는 dev-login 응답 캐시로 충분
- 온보딩 슬라이드, 카카오 알림톡, FCM 푸시
- OAuth 실제 통합 (현재는 dev-login으로 인증 검증)

## 3. 설계 결정 요약

| ID | 결정 | 근거 |
|----|------|------|
| D1 | 범위는 "팀 생성 + 가입 + 상세 + 멤버 목록"까지 | PRD P0 핵심 + 출석 등 다음 feature가 의존하는 기반 |
| D2 | 메인 탭 셸 + 팀 영역을 함께 구축 | UI/UX 미정 상태에서 셸 구조도 한 번에 결정 |
| D3 | 메인 탭 = 홈 + 마이페이지 (2탭, 확장 가능) | 사용자 요청. 향후 용병게임·경기 등 추가 자리 |
| D4 | 디자인 시스템은 기본 SwiftUI로 시작 | UI/UX 미정 시점의 컴포넌트 래퍼는 과투자 |
| D5 | 빈 상태는 인라인 CTA 방식 | 화면이 자기 상태(empty/loaded) 자체 관리, SwiftUI 패턴 부합 |
| D6 | 권한 분기는 CAPTAIN 초대 코드 노출만 | 멤버 관리·팀 정보 수정은 다음 사이클로 묶음 |
| D7 | 팀 가입은 1단계 즉시 가입 | 초대 코드 20자 유니크로 충돌 위험 낮음, 백엔드 추가 작업 회피 |
| D8 | 인증 강제 X — Deferred Auth 모델 | 사용자 요청. 비로그인 둘러보기 + 필요 시 인증 |
| D9 | 인트로는 단순 스플래시 (1~1.5초 자동 전환) | 온보딩 콘텐츠 미정 상태의 최소 형태 |
| D10 | RootView 제거, `DugoutApp` → `SplashView` → `MainTabView` 직진 | 사용자 요청. 분기 단순화 |

## 4. 진입 흐름

```
DugoutApp
  └─ @State isReady
       ├─ false → SplashView (1~1.5초 후 isReady = true)
       └─ true  → MainTabView
                   ├─ [홈]      HomeView
                   └─ [마이페이지] MyPageView
```

- 인증 상태와 무관하게 메인 진입
- `AuthState`는 `@Observable` 환경 객체로 모든 탭이 공유
- 앱 시작 시 `AuthState.boot()` — Keychain의 토큰 복원, 있으면 `isAuthenticated = true`

## 5. 인증 모델 — Deferred Auth

### AuthState

```swift
@Observable
@MainActor
final class AuthState {
    private(set) var currentUser: AuthUser?
    var isAuthenticated: Bool { currentUser != nil }

    func boot() async         // 앱 시작 시 Keychain에서 토큰 복원
    func loginWithDev(...)    // dev-login 호출 → currentUser 설정
    func logout() async       // 토큰 삭제, currentUser = nil
    func handleUnauthorized() // 401 감지 시 호출 (자동 비로그인)
}
```

- `AuthUser`는 dev-login 응답에 담긴 최소 정보(userId, email, displayName)를 보관
- `APIClient`가 401 수신 시 `AuthState.handleUnauthorized()` 호출하도록 연동

### LoginSheet (공용 컴포넌트)

- `Features/Auth/Sources/Presentation/LoginSheet.swift`
- 어떤 화면에서든 `.sheet(isPresented:)` 로 띄울 수 있는 단일 진입점
- 내부에서 dev-login 호출 → 성공 시 `AuthState` 업데이트 → 자동 dismiss
- 향후 OAuth(카카오/네이버 등) 옵션 추가 자리

### Deferred Auth 트리거 패턴

각 ViewModel(`HomeViewModel` 등)이 인증 필요 액션 시 다음 패턴을 따른다.

```swift
@Observable @MainActor
final class HomeViewModel {
    enum PendingAction { case createTeam, joinTeam }

    var showLoginSheet = false
    var pendingAction: PendingAction?
    var presentedSheet: PresentedSheet?  // .createTeam, .joinTeam, nil

    func tapCreateTeam() {
        if auth.isAuthenticated {
            presentedSheet = .createTeam
        } else {
            pendingAction = .createTeam
            showLoginSheet = true
        }
    }

    // View가 onChange(of: auth.isAuthenticated)로 호출
    func onAuthChanged() {
        guard auth.isAuthenticated, let action = pendingAction else { return }
        switch action {
        case .createTeam: presentedSheet = .createTeam
        case .joinTeam:   presentedSheet = .joinTeam
        }
        pendingAction = nil
    }
}
```

이 패턴으로 비로그인 사용자가 액션을 시도해도 로그인 후 끊김 없이 원래 흐름이 이어진다.

## 6. 화면 구성

```
MainTabView
├─ [홈] NavigationStack
│   └─ HomeView
│       ├─ Sheet: CreateTeamView    (팀 만들기 폼)
│       ├─ Sheet: JoinTeamView      (초대 코드 입력)
│       ├─ Sheet: LoginSheet        (deferred auth)
│       └─ Push:  TeamDetailView    (팀 상세 + 멤버)
└─ [마이페이지]
    └─ MyPageView
        └─ Sheet: LoginSheet        (비로그인 시 로그인 진입)
```

### 화면별 책임

| 화면 | 비로그인 | 로그인 | 의존 |
|------|----------|--------|------|
| `HomeView` | 안내 + `[팀 만들기]` `[팀 가입하기]` | 내 팀 목록 + 빈 상태 CTA + 팀 진입 | `TeamRepository.fetchMyTeams()` |
| `CreateTeamView` | (deferred auth) | 폼 입력 → 생성 → dismiss & 목록 새로고침 | `createTeam(_:)` |
| `JoinTeamView` | (deferred auth) | 코드 입력 → 즉시 가입 → dismiss & 새로고침 | `joinTeam(inviteCode:)` |
| `TeamDetailView` | (목록에서 진입 자체 불가) | 팀 정보 + 멤버 목록 + (CAPTAIN) 초대 코드 영역 | `fetchTeam`, `fetchMembers`, `generateInviteCode` |
| `MyPageView` | "로그인" CTA | 사용자 정보 + 로그아웃 | `AuthState` |

### 네비게이션 패턴 결정

- 팀 생성/가입 = **Sheet** (새 객체 생성, iOS HIG 부합, 취소/완료 명확)
- 팀 상세 = **Push** (같은 문맥의 깊이)
- LoginSheet = **Sheet** (모달 인터럽션, 명확한 시작/종료)

## 7. 디렉토리 구조

```
dugout-ios/
├── App/Sources/
│   ├── DugoutApp.swift              # 진입점 (수정)
│   ├── SplashView.swift             # 신규
│   └── MainTabView.swift            # 신규
│
├── Core/                            # 공유 모듈 (기존)
│   └── Network/                     # APIClient, APIError
│
├── Features/
│   ├── Auth/Sources/
│   │   ├── Domain/                  # 기존
│   │   ├── Data/                    # 기존
│   │   └── Presentation/
│   │       ├── AuthState.swift      # 신규 (다른 feature에서 import)
│   │       └── LoginSheet.swift     # 신규
│   │
│   ├── Team/Sources/
│   │   ├── Domain/
│   │   │   ├── Entities/
│   │   │   │   ├── Team.swift              # 기존
│   │   │   │   ├── TeamMember.swift        # 기존 (신규 추가됨)
│   │   │   │   └── MyTeam.swift            # 신규
│   │   │   └── Repositories/
│   │   │       └── TeamRepository.swift    # fetchMyTeams 추가
│   │   ├── Data/
│   │   │   ├── DTOs/
│   │   │   │   ├── TeamDTO.swift           # 기존
│   │   │   │   ├── TeamMemberDTO.swift     # 기존 (신규 추가됨)
│   │   │   │   ├── TeamRequestDTO.swift    # 기존 (신규 추가됨)
│   │   │   │   └── MyTeamDTO.swift         # 신규
│   │   │   └── Repositories/
│   │   │       └── TeamRepositoryImpl.swift # fetchMyTeams 구현
│   │   └── Presentation/
│   │       ├── CreateTeam/
│   │       │   ├── CreateTeamViewModel.swift
│   │       │   └── CreateTeamView.swift
│   │       ├── JoinTeam/
│   │       │   ├── JoinTeamViewModel.swift
│   │       │   └── JoinTeamView.swift
│   │       └── TeamDetail/
│   │           ├── TeamDetailViewModel.swift
│   │           └── TeamDetailView.swift
│   │
│   ├── Home/Sources/Presentation/   # 신규 모듈 (Team Feature import)
│   │   ├── HomeViewModel.swift      # 팀 목록 fetch + deferred auth + sheet 트리거
│   │   └── HomeView.swift           # 내 팀 목록 List 직접 렌더링
│   │
│   └── Profile/Sources/Presentation/ # 신규 모듈
│       ├── MyPageViewModel.swift
│       └── MyPageView.swift
```

> Tuist `Project.swift` 갱신: Home, Profile 모듈 신규 추가. Team 모듈은 기존 의존성 유지.

## 8. 데이터 흐름

### iOS 신규 추가

**`MyTeam` Entity (Domain)**

```swift
public struct MyTeam: Sendable, Equatable, Identifiable {
    public let id: Int64                // teamId
    public let name: String
    public let region: String
    public let division: Int
    public let role: TeamRole           // 내 역할 (CAPTAIN/MANAGER/ACCOUNTANT/MEMBER)
    public let memberCount: Int
}
```

**`MyTeamDTO` (Data)**

```swift
struct MyTeamDTO: Decodable {
    let teamId: Int64
    let name: String
    let region: String
    let division: Int
    let role: String
    let memberCount: Int

    func toDomain() -> MyTeam { ... }
}
```

**`TeamRepository.fetchMyTeams()` 추가**

```swift
public protocol TeamRepository: Sendable {
    // 기존 메서드들...
    func fetchMyTeams() async throws -> [MyTeam]
}

// TeamRepositoryImpl
public func fetchMyTeams() async throws -> [MyTeam] {
    let endpoint = APIEndpoint(path: "/api/v1/users/me/teams")
    let dtos: [MyTeamDTO] = try await client.request(endpoint)
    return dtos.map { $0.toDomain() }
}
```

### Repository ↔ 백엔드 매핑 (변경 없음)

| 메서드 | 백엔드 |
|--------|--------|
| `fetchMyTeams()` (신규) | `GET /api/v1/users/me/teams` |
| `createTeam(_:)` | `POST /api/v1/teams` |
| `joinTeam(inviteCode:)` | `POST /api/v1/teams/join` |
| `fetchTeam(id:)` | `GET /api/v1/teams/{id}` |
| `fetchMembers(teamId:)` | `GET /api/v1/teams/{teamId}/members` |
| `generateInviteCode(teamId:)` | `POST /api/v1/teams/{teamId}/invite` |

## 9. 상태 / 에러 패턴

### ViewState

모든 ViewModel이 동일한 형태의 상태를 가진다.

```swift
enum ViewState<T: Sendable>: Sendable {
    case idle
    case loading
    case loaded(T)
    case failed(String)
}
```

- ViewModel은 `@Observable @MainActor`
- 비동기 작업은 `Task { ... }` 내부에서 `do/catch`로 에러를 `failed`로 변환

### 에러 처리 규칙

- 사용자 입력 단계 검증(빈 값, 형식 오류)은 ViewModel에서 즉시 처리, 네트워크 호출 전 차단
- 네트워크 에러는 `APIError` → 사용자 표시 메시지로 매핑
- 401 → `AuthState.handleUnauthorized()` 호출 후 `failed("로그인이 필요합니다")`로 노출
- 4xx/5xx → 도메인별 메시지 (예: 이미 가입된 팀, 잘못된 초대 코드 등)
- 모든 에러 시나리오는 `do/catch`로 명시적으로 잡고, View에서 alert 또는 인라인 에러로 표시

### 동시성 (Swift 6 Strict Concurrency 준수)

- 모든 도메인 타입 `Sendable`
- ViewModel은 `@MainActor` 격리
- Repository는 `Sendable` protocol, 구현체는 value type (`APIClient` 의존성만 보유)
- 네트워크 호출은 `async throws` + `Task` 패턴

## 10. 작업 분할 (마일스톤)

### M1. iOS Domain/Data 보강 + 메인 셸 골격
- `MyTeam` Entity, `MyTeamDTO`
- `TeamRepository.fetchMyTeams()` + 구현
- `SplashView`, `MainTabView` 골격 (홈/마이페이지 placeholder)
- `DugoutApp` 진입점 수정 (RootView 제거)
- Tuist 프로젝트에 Home, Profile 모듈 신규 등록
- **검증**: 앱 실행 → 스플래시 → 빈 탭 2개 표시

### M2. 인증 통합 + 마이페이지
- `AuthState` `@Observable` 클래스 + 환경 주입
- `AuthState.boot()`로 Keychain 토큰 복원
- `LoginSheet` 컴포넌트 (dev-login 호출, 성공 시 자동 dismiss)
- `MyPageView`: 비로그인 시 로그인 CTA / 로그인 시 정보 + 로그아웃
- `APIClient` 401 감지 → `AuthState.handleUnauthorized()` 연동
- **검증**: 마이페이지에서 로그인 시트 → dev-login → 이메일 표시 → 로그아웃 → 비로그인 복귀

### M3. 홈 탭 + 팀 생성/가입 (Deferred Auth)
- `HomeViewModel`, `HomeView` (비로그인 안내 / 로그인 시 내 팀 목록 — `fetchMyTeams` 호출 + 빈 상태 + sheet 트리거)
- `CreateTeamView` + `CreateTeamViewModel` (sheet, 폼 입력 + 검증)
- `JoinTeamView` + `JoinTeamViewModel` (sheet)
- Deferred auth 트리거 패턴 적용 (`pendingAction` + `onAuthChanged`)
- **검증**: 비로그인 → 팀 만들기 → 로그인 시트 → 로그인 → 자동으로 생성 폼 → 생성 → 홈 목록에 표시

### M4. 팀 상세 + 멤버 목록 + 초대 코드
- `TeamDetailViewModel`, `TeamDetailView`
- 팀 정보 섹션 (이름/지역/부수)
- 멤버 목록 List (이름/등번호/포지션/역할)
- CAPTAIN 한정 초대 코드 영역 (`generateInviteCode` 호출 + 복사 버튼)
- 권한 판단: 현재 사용자의 멤버 정보에서 `role == .captain` 조회
- **검증**: 다른 dev 계정으로 가입 → 두 계정 멤버 목록에 표시 + CAPTAIN 시점만 초대 코드 노출

## 11. End-to-End 검증 (M4 완료 시점)

1. 앱 실행 → 스플래시 → 홈 탭 (비로그인 안내 표시)
2. 홈에서 `[팀 만들기]` 탭 → LoginSheet 자동 표시 → dev-login → 자동으로 CreateTeamView sheet 진입
3. 폼 입력 → 생성 → 홈 목록에 새 팀 표시
4. 팀 카드 탭 → TeamDetailView push → 초대 코드 영역 노출 (CAPTAIN) → 복사
5. 마이페이지 → 로그아웃 → 비로그인 복귀
6. 다른 dev 계정으로 dev-login → `[팀 가입하기]` → 코드 입력 → 가입 성공
7. 홈에 가입 팀 표시 → 상세 진입 → 멤버 2명 표시 + 초대 코드 영역 숨김 (MEMBER)

## 12. 위험 & 완화

| 위험 | 완화 |
|------|------|
| dev-login 응답 형식이 `AuthState`가 기대하는 형태와 다를 수 있음 | M2 시작 시 dev-login 응답 스키마 확인 → `AuthUser` 매핑 결정 |
| `MyTeamResponse` 백엔드 DTO 필드와 iOS `MyTeamDTO` 불일치 가능 | M1에서 백엔드 `MyTeamResponse.kt` 직접 확인 후 `MyTeamDTO` 작성 |
| 401 자동 처리에서 무한 루프(refresh 시도 중 401) | `APIClient`에서 refresh 경로는 401 핸들러 우회 |
| Tuist 모듈 신규 추가 후 빌드 실패 | M1에서 `tuist generate` 검증 후 다음 마일스톤 진행 |
| Deferred auth에서 `pendingAction` 누수(로그인 후 다른 액션을 탭) | View에서 `onAuthChanged` 호출 시 `pendingAction` 즉시 nil 처리 |

## 13. 후속 사이클

- 팀 정보 수정 + 멤버 추방·역할 변경 (한 사이클로 묶어 권한 분기 패턴 정립)
- DG 디자인 시스템 (Colors, Spacing, Typography 토큰 → DGButton 등 컴포넌트)
- 팀 가입 미리보기 (백엔드 `GET /teams/by-invite-code/{code}` + iOS UX 개선)
- 백엔드 `GET /api/v1/users/me` (사용자 프로필 풍부화 시점)
- 일정·출석 feature (홈 대시보드에 위젯으로 통합)
- OAuth 실제 통합 (카카오/네이버/구글/애플)
