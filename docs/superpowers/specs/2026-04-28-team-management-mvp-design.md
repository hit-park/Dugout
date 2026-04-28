# Team Management MVP — 팀 정보 수정 · 멤버 관리 · 세션 복원 설계

## 1. 목적

직전 Team Feature MVP에서 만든 팀 생성/가입 흐름을 사용자가 실제로 **운영 가능한 상태**로 보강한다. Phase 1.5 성격.

### 묶인 작업

- **A1. 팀 정보 수정** — CAPTAIN/MANAGER가 팀 이름/지역/부수/활동 요일·시간/라인업 모드를 수정.
- **A2. 멤버 역할 변경 · 추방** — CAPTAIN이 다른 멤버의 role을 manager/accountant/member 사이에서 변경하거나 추방.
- **B5. 세션 rehydration** — 앱 재시작 시 토큰이 살아있으면 `GET /users/me`로 user 정보를 받아 `state = .authenticated(user)` 자동 복원. 그 전에 백엔드 `/users/me` 신규 추가 포함.

새 도메인 추가 없음. 백엔드 endpoint 1개 신규 + iOS Presentation 레이어 확장 + Auth 흐름 보강.

## 2. 코드 베이스 현황

### 이미 구현된 것 (재사용)

- 백엔드:
  - `PUT /api/v1/teams/{id}` — 팀 정보 수정 (CAPTAIN+MANAGER 허용)
  - `PUT /api/v1/teams/{teamId}/members/{memberId}` — 멤버 정보 수정 (CAPTAIN 허용)
  - `DELETE /api/v1/teams/{teamId}/members/{memberId}` — 멤버 추방 (CAPTAIN 허용, CAPTAIN 본인은 거부)
  - `JwtFilter`가 `@AuthenticationPrincipal Long`으로 userId 주입
  - `AuthService` (oauthLogin / devLogin / refresh / logout)
- iOS:
  - `TeamRepository` (createTeam, fetchTeam, fetchMembers, generateInviteCode, joinTeam)
  - `TeamDetailViewModel` / `TeamDetailView` (팀 정보 + 초대 코드 + 멤버 목록, CAPTAIN 한정 invite section)
  - `LoginSheet`, `MyPageView`, `HomeView` Deferred Auth 흐름
  - `AuthViewModel.checkAuthStatus` (단순 토큰 존재 여부만 검사 — 본 plan에서 보강)
  - `APIClient` + `AuthInterceptor` (401 자동 refresh)
  - `JSONCoder` LocalDateTime fractional seconds 폴백
  - `AuthProvider.displayName`, `DayOfWeek`, `TeamRole.displayName`

### 신규로 만들 것

- 백엔드:
  - `domain/user/controller/UserController.kt`
  - `domain/user/service/UserService.kt`
  - `domain/user/dto/UserResponse.kt`
- iOS:
  - `Core/Network/Sources/Concurrency.swift` (`withTimeout`, `TimeoutError`)
  - `Features/Team/Sources/Presentation/ViewModels/EditTeamViewModel.swift`
  - `Features/Team/Sources/Presentation/Views/EditTeamView.swift`

### 수정할 것

- 문서:
  - `docs/TDD.md` — API 설계에 `GET /users/me` 추가
- iOS:
  - `Features/Auth/Sources/Domain/Repositories/AuthRepository.swift` — `fetchMe()` 추가
  - `Features/Auth/Sources/Data/Repositories/AuthRepositoryImpl.swift` — `fetchMe` 구현
  - `Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift` — `checkAuthStatus` 보완
  - `App/Sources/SplashView.swift` — sleep 제거, 단순 정적 view
  - `App/Sources/DugoutApp.swift` — task에서 `checkAuthStatus` 후 `isReady = true`
  - `Features/Team/Sources/Domain/Entities/TeamMember.swift` — `TeamRole` 권한 extension
  - `Features/Team/Sources/Domain/Repositories/TeamRepository.swift` — `updateTeam`, `updateMember`, `removeMember` + `UpdateTeamRequest`
  - `Features/Team/Sources/Data/Repositories/TeamRepositoryImpl.swift` — 위 3개 구현
  - `Features/Team/Sources/Data/DTOs/TeamRequestDTO.swift` — `UpdateTeamRequestDTO`, `UpdateMemberRequestDTO`
  - `Features/Team/Sources/Presentation/ViewModels/TeamDetailViewModel.swift` — 권한 + 멤버 액션 확장
  - `Features/Team/Sources/Presentation/Views/TeamDetailView.swift` — 편집 진입점, 멤버 row Button 분기, ConfirmationDialog, alert

## 3. 마일스톤별 설계

### M1. 백엔드 — `GET /api/v1/users/me`

위치 결정: AuthController는 인증 액션(`/auth/*`)만 다루므로 user 조회는 **새 UserController로 분리**한다. 추후 사용자 프로필 관련 엔드포인트가 생길 자리.

```kotlin
// UserController.kt
@RestController
@RequestMapping("/api/v1")
class UserController(private val userService: UserService) {
    @GetMapping("/users/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getMe(userId))
}

// UserService.kt
@Service
@Transactional(readOnly = true)
class UserService(private val userRepository: UserRepository) {
    fun getMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return UserResponse.from(user)
    }
}

// UserResponse.kt
data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImgUrl: String?,
    val provider: String,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            profileImgUrl = user.profileImgUrl,
            provider = user.provider.name,
        )
    }
}
```

JSON 직렬화 결과는 기존 `AuthResponse.user`와 동일 (snake_case + 같은 필드). iOS `UserDTO`가 그대로 받는다.

`docs/TDD.md` API 설계 섹션에 `GET /users/me`를 추가한다 (인증 필요, 200 OK + UserResponse).

### M2. iOS — 세션 rehydration

**Domain — `AuthRepository.fetchMe()`**

```swift
public protocol AuthRepository: Sendable {
    // ... 기존
    func fetchMe() async throws -> User
}
```

**Data — `AuthRepositoryImpl.fetchMe`**

```swift
public func fetchMe() async throws -> User {
    let endpoint = APIEndpoint(path: "/api/v1/users/me", requiresAuth: true)
    let dto: UserDTO = try await client.request(endpoint)
    return dto.toDomain()
}
```

**`Concurrency.swift` (Core/Network 신규)**

```swift
public struct TimeoutError: Error, Sendable {}

public func withTimeout<T: Sendable>(
    seconds: TimeInterval,
    _ operation: @escaping @Sendable () async throws -> T
) async throws -> T {
    try await withThrowingTaskGroup(of: T.self) { group in
        group.addTask { try await operation() }
        group.addTask {
            try await Task.sleep(for: .seconds(seconds))
            throw TimeoutError()
        }
        let result = try await group.next()!
        group.cancelAll()
        return result
    }
}
```

**`AuthViewModel.checkAuthStatus` 보완**

```swift
public func checkAuthStatus() async {
    let authenticated = await tokenStore.isAuthenticated
    guard authenticated else {
        state = .idle
        return
    }
    do {
        let user = try await withTimeout(seconds: 5) {
            try await repository.fetchMe()
        }
        state = .authenticated(user)
    } catch APIError.unauthorized {
        await tokenStore.clear()
        state = .idle
    } catch {
        // timeout / 네트워크 에러 → 토큰 유지, 비로그인 진입.
        // 사용자가 다시 시도하면 LoginSheet로 새 토큰 발급, 다음 cold start에 재시도.
        state = .idle
    }
}
```

**`SplashView`** — sleep 제거. 단순 정적 view (로고 + 텍스트). 진입 트리거는 외부.

**`DugoutApp.task`** — `checkAuthStatus` 완료 후 `isReady = true`로 메인 진입.

```swift
.task {
    await authViewModel.checkAuthStatus()
    isReady = true
}
```

응답 시간(보통 100~500ms) = 사용자 체감 splash 시간. cold start 가속. timeout 시 5초 cap. 깜빡임 방지용 min duration은 두지 않음 (사용자가 1.2s sleep 자체를 군더더기로 본 결정 반영).

### M3. 권한 매트릭스 일치

`TeamRole`에 권한 helper extension을 도입해 도메인에 권한을 응집시키고, View/ViewModel은 이를 사용한다.

```swift
// TeamMember.swift
public extension TeamRole {
    var canEditTeam: Bool       { self == .captain || self == .manager }
    var canShowInviteCode: Bool { self == .captain || self == .manager }
    var canManageMembers: Bool  { self == .captain }
}
```

**`TeamDetailViewModel` 갱신**:

```swift
public var canShowInviteCode: Bool { myRole?.canShowInviteCode ?? false }  // 기존 구현 교체
public var canEditTeam: Bool       { myRole?.canEditTeam ?? false }         // 신규
public var canManageMembers: Bool  { myRole?.canManageMembers ?? false }    // 신규
```

기존 `canShowInviteCode`의 동작은 의도적으로 변경된다 — MANAGER도 true. 백엔드(`PUT /teams/{id}/invite`가 CAPTAIN+MANAGER 허용)와 일치.

### M4. 팀 정보 수정

**Domain — `UpdateTeamRequest` + `TeamRepository.updateTeam`**

```swift
public struct UpdateTeamRequest: Sendable, Equatable {
    public let name: String?
    public let region: String?
    public let division: Int?
    public let activityDays: [String]?
    public let activityTime: String?
    public let lineupMode: LineupMode?

    public init(
        name: String? = nil,
        region: String? = nil,
        division: Int? = nil,
        activityDays: [String]? = nil,
        activityTime: String? = nil,
        lineupMode: LineupMode? = nil
    ) { /* ... */ }
}

public protocol TeamRepository: Sendable {
    // ... 기존
    func updateTeam(id: Int64, request: UpdateTeamRequest) async throws -> Team
}
```

**Data — `TeamRepositoryImpl.updateTeam`**

```swift
public func updateTeam(id: Int64, request: UpdateTeamRequest) async throws -> Team {
    let body = UpdateTeamRequestDTO(
        name: request.name,
        region: request.region,
        division: request.division,
        activityDays: request.activityDays,
        activityTime: request.activityTime,
        lineupMode: request.lineupMode?.rawValue
    )
    let endpoint = APIEndpoint.json(
        path: "/api/v1/teams/\(id)",
        method: .put,
        body: body
    )
    let dto: TeamDTO = try await client.request(endpoint)
    return dto.toDomain()
}
```

**`UpdateTeamRequestDTO`** (`TeamRequestDTO.swift`에 추가):

```swift
struct UpdateTeamRequestDTO: Encodable, Sendable {
    let name: String?
    let region: String?
    let division: Int?
    let activityDays: [String]?
    let activityTime: String?
    let lineupMode: String?

    enum CodingKeys: String, CodingKey {
        case name, region, division
        case activityDays = "activity_days"
        case activityTime = "activity_time"
        case lineupMode = "lineup_mode"
    }
}
```

**`EditTeamViewModel` (Presentation 신규)**

`CreateTeamViewModel`과 비슷한 구조. 차이점:
- `init(team: Team, repository: any TeamRepository = TeamRepositoryImpl())` — 폼 default를 기존 Team 값으로 채움 (`activityDays`는 `[String] → Set<DayOfWeek>` 매핑 필요).
- `submit()` → `repository.updateTeam(team.id, request)` 호출.
- partial update 의도가 없으면 모든 필드 그대로 보냄(빈 문자열은 nil로 trim).

**`EditTeamView` (Presentation 신규)**

`CreateTeamView`와 거의 동일한 Form. 차이점:
- `navigationTitle("팀 정보 수정")`
- toolbar 좌측 "취소" / 우측 "저장" 버튼 (CreateTeamView 패턴 동일, submitting 시 ProgressView)
- 에러 Section 상단 노출
- `onCompleted` 콜백으로 외부(TeamDetailView)에서 reload 트리거

**`TeamDetailView` 진입점**

```swift
@State private var showEditSheet = false
// ...
.toolbar {
    if viewModel.canEditTeam, case .loaded = viewModel.state {
        ToolbarItem(placement: .topBarTrailing) {
            Button("편집") { showEditSheet = true }
        }
    }
}
.sheet(isPresented: $showEditSheet) {
    if case .loaded(let data) = viewModel.state {
        EditTeamView(viewModel: EditTeamViewModel(team: data.team)) {
            await viewModel.load()
        }
    }
}
```

저장 성공 → `onCompleted` (= `viewModel.load()` 재호출) → 시트 dismiss → TeamDetailView가 갱신된 정보로 재렌더.

### M5. 멤버 관리 (역할 변경 · 추방)

**Domain — repository 메서드 2개**

```swift
public protocol TeamRepository: Sendable {
    // ... 기존 + M4
    func updateMember(teamId: Int64, memberId: Int64, role: TeamRole) async throws -> TeamMember
    func removeMember(teamId: Int64, memberId: Int64) async throws
}
```

**Data**

```swift
public func updateMember(teamId: Int64, memberId: Int64, role: TeamRole) async throws -> TeamMember {
    let body = UpdateMemberRequestDTO(role: role.rawValue)
    let endpoint = APIEndpoint.json(
        path: "/api/v1/teams/\(teamId)/members/\(memberId)",
        method: .put,
        body: body
    )
    let dto: TeamMemberDTO = try await client.request(endpoint)
    return dto.toDomain()
}

public func removeMember(teamId: Int64, memberId: Int64) async throws {
    let endpoint = APIEndpoint(
        path: "/api/v1/teams/\(teamId)/members/\(memberId)",
        method: .delete
    )
    try await client.requestVoid(endpoint)
}
```

**`UpdateMemberRequestDTO`**: 단일 필드 `role: String`.

**`TeamDetailViewModel` 확장**

```swift
public var selectedMember: TeamMember?
public var memberActionError: String?
public private(set) var isMemberActionInFlight: Bool = false

public func isMemberActionable(_ member: TeamMember) -> Bool {
    guard canManageMembers else { return false }
    if member.role == .captain { return false }
    if member.userId == currentUserId { return false }
    return true
}

public func tapMember(_ member: TeamMember) {
    guard isMemberActionable(member) else { return }
    selectedMember = member
}

public func updateMemberRole(_ role: TeamRole) async {
    guard let member = selectedMember else { return }
    isMemberActionInFlight = true
    defer {
        isMemberActionInFlight = false
        selectedMember = nil
    }
    do {
        _ = try await repository.updateMember(teamId: teamId, memberId: member.id, role: role)
        await load()
    } catch let error as APIError {
        memberActionError = error.userMessage
    } catch {
        memberActionError = "역할 변경에 실패했습니다"
    }
}

public func removeMember() async {
    guard let member = selectedMember else { return }
    isMemberActionInFlight = true
    defer {
        isMemberActionInFlight = false
        selectedMember = nil
    }
    do {
        try await repository.removeMember(teamId: teamId, memberId: member.id)
        await load()
    } catch let error as APIError {
        memberActionError = error.userMessage
    } catch {
        memberActionError = "추방에 실패했습니다"
    }
}
```

**`TeamDetailView` 변경**

기존 멤버 row 코드를 `private struct MemberRow: View`로 추출 (재사용/가독성).

```swift
ForEach(members) { member in
    if viewModel.isMemberActionable(member) {
        Button { viewModel.tapMember(member) } label: {
            MemberRow(member: member)
        }
        .buttonStyle(.plain)
    } else {
        MemberRow(member: member)
    }
}
.confirmationDialog(
    viewModel.selectedMember?.nickname ?? "",
    isPresented: Binding(
        get: { viewModel.selectedMember != nil },
        set: { if !$0 { viewModel.selectedMember = nil } }
    ),
    presenting: viewModel.selectedMember
) { _ in
    Button("매니저로 변경")  { Task { await viewModel.updateMemberRole(.manager) } }
    Button("회계로 변경")   { Task { await viewModel.updateMemberRole(.accountant) } }
    Button("일반으로 변경") { Task { await viewModel.updateMemberRole(.member) } }
    Button("추방", role: .destructive) { Task { await viewModel.removeMember() } }
    Button("취소", role: .cancel) {}
}
.alert(
    "오류",
    isPresented: Binding(
        get: { viewModel.memberActionError != nil },
        set: { if !$0 { viewModel.memberActionError = nil } }
    ),
    presenting: viewModel.memberActionError
) { _ in
    Button("확인") { viewModel.memberActionError = nil }
} message: { Text($0) }
```

비활성 row(자기 자신, CAPTAIN, MEMBER 시점 등)는 Button wrap 없이 단순 display — 시각 dim 없이 "탭 무반응"이 자연스러움.

## 4. 권한 매트릭스 (정리)

| 액션 | 백엔드 허용 | iOS (이번 plan 적용 후) |
|---|---|---|
| 팀 정보 수정 | CAPTAIN + MANAGER | `canEditTeam` (CAPTAIN + MANAGER) |
| 초대 코드 생성/조회 | CAPTAIN + MANAGER | `canShowInviteCode` (CAPTAIN + MANAGER) |
| 멤버 역할 변경 | CAPTAIN | `canManageMembers` (CAPTAIN) |
| 멤버 추방 | CAPTAIN (단, CAPTAIN 본인은 거부) | `canManageMembers` + 자기 자신/CAPTAIN row 비활성 |

iOS는 모든 비활성 시 진입점 자체를 숨김/disable. 백엔드도 권한 검증 그대로 유지 (defense-in-depth).

## 5. 비기능 요구사항

- Swift 6 Strict Concurrency: 모든 새 타입 `Sendable`, ViewModel `@MainActor @Observable`.
- 한국어 메시지 일관성 (백엔드 `ErrorResponse.message` 우선, fallback 한국어 고정 문구).
- 빌드 검증: 각 milestone 종료 시 `xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build` BUILD SUCCEEDED.
- 단위 테스트 타겟 없음 — 검증은 빌드 + 시뮬레이터 시나리오.
- TDD.md API 설계 섹션 동기화 (M1 신규 endpoint).

## 6. 검증 시나리오 (M5 완료 시점)

### Prerequisites

- 백엔드 local profile (`./gradlew bootRun --args='--spring.profiles.active=local'`) 8080.
- 시뮬레이터 Erase All Content (앱 캐시·토큰 초기화).

### 통합 시나리오 (14단계)

**A. 세션 rehydration**

1. "주장A"로 dev-login → 팀 "테스트팀" 생성 → 홈 카드 + 마이페이지에 사용자 정보 표시.
2. 앱 강제 종료 (Cmd+Shift+H 두 번 → 위로 swipe).
3. 앱 다시 실행 → splash → **재로그인 없이** 홈에 카드 + 마이페이지에 "주장A" 자동 복원.

**B. 팀 정보 수정 + CAPTAIN 권한**

4. 카드 탭 → TeamDetailView → toolbar **"편집" 버튼 노출** (CAPTAIN).
5. 편집 → EditTeamView 시트 → 이름/지역/부수/활동 요일 변경 → 저장 → 시트 dismiss → TeamDetailView 갱신.

**C. 멤버 가입 + MEMBER 권한**

6. 초대 코드 생성 → 메모 → 마이페이지 로그아웃.
7. "용병B" 로그인 → 초대 코드로 가입 → 홈 카드 표시.
8. 카드 탭 → 멤버 (2) + **편집 버튼 미표시 + 초대 코드 영역 미표시** (MEMBER).
9. **멤버 row 탭 무반응** (`canManageMembers == false`).

**D. CAPTAIN의 멤버 관리**

10. 로그아웃 → "주장A" 재로그인 → 카드 탭 → 용병B row 탭 → ConfirmationDialog → "매니저로 변경" → reload → 배지 "매니저".
11. **본인(주장A) row 탭 무반응** (자기 자신 비활성).

**E. MANAGER 권한**

12. 로그아웃 → "용병B" 재로그인 → 카드 탭
    - **편집 버튼 노출** (MANAGER → `canEditTeam`).
    - **초대 코드 영역 노출** (MANAGER → `canShowInviteCode`).
    - **멤버 row 탭 여전히 무반응** (`canManageMembers == false`; CAPTAIN 전용).

**F. 추방**

13. 로그아웃 → "주장A" 재로그인 → 카드 탭 → 용병B row 탭 → "추방" → reload → 멤버 (1).
14. 마이페이지 로그아웃 → 비로그인 복귀.

## 7. Scope 외 (후속 작업 후보)

- CAPTAIN 권한 위임 (백엔드 자동 강등 로직 필요)
- jersey number / position 수정 (`UpdateMemberRequest`의 다른 필드)
- 팀 logo 업로드 (이미지 처리)
- 팀 탈퇴 (member 본인이 deactivate)
- TDD.md iOS 아키텍처 섹션 정비 (별도 D 작업)
