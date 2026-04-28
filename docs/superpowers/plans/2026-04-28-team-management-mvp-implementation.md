# Team Management MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 팀 정보 수정 + 멤버 관리(역할 변경/추방) + 세션 rehydration을 추가해, 직전 MVP에서 만든 팀이 실제 운영 가능한 상태가 되게 한다.

**Architecture:** 백엔드 `GET /users/me` 1개 신규(UserController). iOS는 `AuthViewModel.checkAuthStatus` 보강 + `SplashView` 단순화 + `TeamRole` 권한 extension + `EditTeamViewModel/View` + `TeamDetailViewModel` 멤버 액션 확장. 각 milestone은 빌드 검증을 끝낸 commit으로 닫는다.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, @Observable, Tuist 4.x, Alamofire, Spring Boot 3 + Kotlin

---

## 0. 사전 준비 — 코드 베이스 현황 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령

```bash
# iOS
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build

# 백엔드
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew build -x test
./gradlew bootRun --args='--spring.profiles.active=local'   # 로컬 실행
```

> 단위 테스트 타겟이 없으므로 각 task의 검증 = 빌드 성공. M5 완료 시점에 시뮬레이터 14단계 시나리오 검증.

### 재사용 / 신규 / 수정 파일 (spec §2 참고)

핵심:
- 신규 (백엔드): `UserController.kt`, `UserService.kt`, `UserResponse.kt`
- 신규 (iOS): `Concurrency.swift`, `EditTeamViewModel.swift`, `EditTeamView.swift`
- 수정 (iOS): `AuthRepository.swift` / `AuthRepositoryImpl.swift` / `AuthViewModel.swift` / `SplashView.swift` / `DugoutApp.swift` / `TeamMember.swift` / `TeamRepository.swift` / `TeamRepositoryImpl.swift` / `TeamRequestDTO.swift` / `TeamDetailViewModel.swift` / `TeamDetailView.swift`
- 수정 (문서): `docs/TDD.md`

---

## Milestone 1 — 백엔드 GET /api/v1/users/me

### Task 1.1: UserResponse DTO

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/user/dto/UserResponse.kt`

- [ ] **Step 1: 파일 작성**

```kotlin
package com.dugout.api.domain.user.dto

import com.dugout.api.domain.user.entity.User

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

---

### Task 1.2: UserService

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/user/service/UserService.kt`

- [ ] **Step 1: 파일 작성**

```kotlin
package com.dugout.api.domain.user.service

import com.dugout.api.domain.user.dto.UserResponse
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(private val userRepository: UserRepository) {
    fun getMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return UserResponse.from(user)
    }
}
```

---

### Task 1.3: UserController + 빌드 + TDD.md 업데이트 + commit

**Files:**
- Create: `dugout-api/src/main/kotlin/com/dugout/api/domain/user/controller/UserController.kt`
- Modify: `docs/TDD.md`

- [ ] **Step 1: UserController 파일 작성**

```kotlin
package com.dugout.api.domain.user.controller

import com.dugout.api.domain.user.dto.UserResponse
import com.dugout.api.domain.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class UserController(private val userService: UserService) {

    @GetMapping("/users/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getMe(userId))
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew build -x test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: TDD.md API 섹션에 추가**

`docs/TDD.md`의 API 설계 섹션(인증/사용자 관련 그룹)에 다음을 추가. 위치는 기존 문서 구조에 맞춰 결정 (예: `## API 설계` 아래 `### 사용자` 또는 인증 관련 섹션 끝).

```markdown
### GET /api/v1/users/me

현재 토큰의 사용자 정보 조회. 세션 복원 / 마이페이지 보강 용도.

- 인증: Bearer JWT 필수
- 응답: 200 OK + `UserResponse { id, email, nickname, profile_img_url, provider }`
- 에러: 401 (토큰 만료/무효), 404 (USER_NOT_FOUND — 토큰 sub가 가리키는 user가 DB에 없음)
```

- [ ] **Step 4: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-api/src/main/kotlin/com/dugout/api/domain/user/ docs/TDD.md
git commit -m "feat(api): GET /users/me 추가 + UserController 신설

- UserResponse / UserService / UserController 신규
- AuthService와 분리 (인증 액션 vs user 조회 책임 분리)
- TDD.md API 섹션 동기화"
```

---

## Milestone 2 — iOS 세션 rehydration

### Task 2.1: Core/Network에 withTimeout 헬퍼 추가

**Files:**
- Create: `dugout-ios/Core/Network/Sources/Concurrency.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  Concurrency.swift
//  DugoutCoreNetwork
//

import Foundation

/// 작업이 지정된 시간 안에 끝나지 않으면 throw.
public struct TimeoutError: Error, Sendable {
    public init() {}
}

/// 비동기 작업에 timeout을 적용한다.
/// operation 또는 sleep 중 먼저 끝나는 쪽이 결과를 반환하고 나머지는 cancel.
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

- [ ] **Step 2: Tuist 갱신 + 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

> 이 task 단독으로는 commit하지 않고 M2 끝(Task 2.4)에 일괄 commit.

---

### Task 2.2: AuthRepository.fetchMe()

**Files:**
- Modify: `dugout-ios/Features/Auth/Sources/Domain/Repositories/AuthRepository.swift`
- Modify: `dugout-ios/Features/Auth/Sources/Data/Repositories/AuthRepositoryImpl.swift`

- [ ] **Step 1: protocol에 fetchMe 추가**

기존:
```swift
public protocol AuthRepository: Sendable {
    func oauthLogin(provider: AuthProvider, accessToken: String) async throws -> User
    func devLogin(nickname: String) async throws -> User
    func logout() async throws
}
```

변경 후:
```swift
public protocol AuthRepository: Sendable {
    func oauthLogin(provider: AuthProvider, accessToken: String) async throws -> User
    func devLogin(nickname: String) async throws -> User
    func logout() async throws
    /// 현재 토큰의 user 정보를 조회. 401 시 APIError.unauthorized.
    func fetchMe() async throws -> User
}
```

- [ ] **Step 2: AuthRepositoryImpl에 구현 추가**

`logout()` 메서드 끝 다음 줄에 추가:

```swift
public func fetchMe() async throws -> User {
    let endpoint = APIEndpoint(path: "/api/v1/users/me", requiresAuth: true)
    let dto: UserDTO = try await client.request(endpoint)
    return dto.toDomain()
}
```

- [ ] **Step 3: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

---

### Task 2.3: AuthViewModel.checkAuthStatus 보완

**Files:**
- Modify: `dugout-ios/Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift`

- [ ] **Step 1: import 확인**

기존 파일 상단의 import 확인. `import DugoutCoreNetwork`이 이미 있어야 함 (`APIError`, `withTimeout` 사용 위해). 없으면 추가.

- [ ] **Step 2: checkAuthStatus 교체**

기존:
```swift
public func checkAuthStatus() async {
    let authenticated = await tokenStore.isAuthenticated
    if !authenticated {
        state = .idle
    }
}
```

변경 후:
```swift
public func checkAuthStatus() async {
    let authenticated = await tokenStore.isAuthenticated
    guard authenticated else {
        state = .idle
        return
    }
    do {
        let user = try await withTimeout(seconds: 5) {
            try await self.repository.fetchMe()
        }
        state = .authenticated(user)
    } catch APIError.unauthorized {
        await tokenStore.clear()
        state = .idle
    } catch {
        // timeout / 네트워크 에러 → 토큰 유지, 비로그인 진입.
        // 다음 cold start에 자동 재시도.
        state = .idle
    }
}
```

- [ ] **Step 3: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

---

### Task 2.4: SplashView 단순화 + DugoutApp 변경 + commit

**Files:**
- Modify: `dugout-ios/App/Sources/SplashView.swift`
- Modify: `dugout-ios/App/Sources/DugoutApp.swift`

- [ ] **Step 1: SplashView 교체**

기존:
```swift
struct SplashView: View {
    let onReady: @MainActor () -> Void

    var body: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "baseball.diamond.bases")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.primary)
            Text("Dugout")
                .font(DGFont.title)
                .foregroundStyle(DGColor.textPrimary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.background)
        .task {
            try? await Task.sleep(for: .milliseconds(1200))
            onReady()
        }
    }
}

#Preview {
    SplashView(onReady: {})
}
```

변경 후:
```swift
struct SplashView: View {
    var body: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "baseball.diamond.bases")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.primary)
            Text("Dugout")
                .font(DGFont.title)
                .foregroundStyle(DGColor.textPrimary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.background)
    }
}

#Preview {
    SplashView()
}
```

- [ ] **Step 2: DugoutApp 변경**

기존:
```swift
@main
struct DugoutApp: App {
    @State private var authViewModel = AuthViewModel()
    @State private var isReady = false

    var body: some Scene {
        WindowGroup {
            Group {
                if isReady {
                    MainTabView(authViewModel: authViewModel)
                } else {
                    SplashView {
                        isReady = true
                    }
                }
            }
            .task {
                await authViewModel.checkAuthStatus()
            }
        }
    }
}
```

변경 후:
```swift
@main
struct DugoutApp: App {
    @State private var authViewModel = AuthViewModel()
    @State private var isReady = false

    var body: some Scene {
        WindowGroup {
            Group {
                if isReady {
                    MainTabView(authViewModel: authViewModel)
                } else {
                    SplashView()
                }
            }
            .task {
                await authViewModel.checkAuthStatus()
                isReady = true
            }
        }
    }
}
```

- [ ] **Step 3: Tuist 재생성 + 빌드 검증**

```bash
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: 시뮬레이터 동작 확인 (선택)**

키체인/UserDefaults에 토큰이 남아 있다면 splash → 자동으로 인증 상태 복원 → 홈에 카드 표시. 토큰 없으면 비로그인 홈.

- [ ] **Step 5: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Core/Network/Sources/Concurrency.swift \
        dugout-ios/Features/Auth/Sources/Domain/Repositories/AuthRepository.swift \
        dugout-ios/Features/Auth/Sources/Data/Repositories/AuthRepositoryImpl.swift \
        dugout-ios/Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift \
        dugout-ios/App/Sources/SplashView.swift \
        dugout-ios/App/Sources/DugoutApp.swift
git commit -m "feat(ios): 세션 rehydration (GET /users/me + checkAuthStatus 보완)

- AuthRepository.fetchMe() 추가
- AuthViewModel.checkAuthStatus가 fetchMe 호출해 state 복원
  - 401: 토큰 정리 + idle
  - timeout/네트워크: 토큰 유지 + idle (다음 cold start에 재시도)
- SplashView 단순 정적 view (sleep 제거)
- DugoutApp.task가 checkAuthStatus await 후 isReady=true
- withTimeout 헬퍼 (Core/Network)"
```

---

## Milestone 3 — 권한 매트릭스 일치

### Task 3.1: TeamRole 권한 helper extension + ViewModel 갱신 + commit

**Files:**
- Modify: `dugout-ios/Features/Team/Sources/Domain/Entities/TeamMember.swift`
- Modify: `dugout-ios/Features/Team/Sources/Presentation/ViewModels/TeamDetailViewModel.swift`

- [ ] **Step 1: TeamMember.swift 끝(파일 마지막)에 extension 추가**

```swift
public extension TeamRole {
    /// 팀 정보 수정 가능 여부 (백엔드 PUT /teams/{id} 권한과 일치).
    var canEditTeam: Bool { self == .captain || self == .manager }

    /// 초대 코드 영역 표시/생성 가능 여부 (백엔드 POST /teams/{id}/invite 권한과 일치).
    var canShowInviteCode: Bool { self == .captain || self == .manager }

    /// 다른 멤버 역할 변경/추방 가능 여부 (백엔드 PUT/DELETE /members/{id} 권한과 일치).
    var canManageMembers: Bool { self == .captain }
}
```

- [ ] **Step 2: TeamDetailViewModel.canShowInviteCode 교체 + 신규 추가**

기존:
```swift
public var canShowInviteCode: Bool {
    myRole == .captain
}
```

변경 후:
```swift
public var canShowInviteCode: Bool { myRole?.canShowInviteCode ?? false }
public var canEditTeam: Bool       { myRole?.canEditTeam ?? false }
public var canManageMembers: Bool  { myRole?.canManageMembers ?? false }
```

> 동작 변경: `canShowInviteCode`가 MANAGER에게도 true가 됨 (의도된 변경, spec §4 권한 매트릭스).

- [ ] **Step 3: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: 커밋**

```bash
git add dugout-ios/Features/Team/Sources/Domain/Entities/TeamMember.swift \
        dugout-ios/Features/Team/Sources/Presentation/ViewModels/TeamDetailViewModel.swift
git commit -m "feat(ios): TeamRole 권한 extension + 매트릭스 일치

- TeamRole.canEditTeam / canShowInviteCode / canManageMembers
- canShowInviteCode가 MANAGER에게도 true (백엔드와 일치)
- TeamDetailViewModel에 canEditTeam, canManageMembers 노출"
```

---

## Milestone 4 — 팀 정보 수정

### Task 4.1: UpdateTeamRequest + DTO + Repository

**Files:**
- Modify: `dugout-ios/Features/Team/Sources/Domain/Repositories/TeamRepository.swift`
- Modify: `dugout-ios/Features/Team/Sources/Data/DTOs/TeamRequestDTO.swift`
- Modify: `dugout-ios/Features/Team/Sources/Data/Repositories/TeamRepositoryImpl.swift`

- [ ] **Step 1: TeamRepository protocol에 메서드 추가**

기존 protocol의 마지막 메서드 다음에 추가:
```swift
/// 팀 정보 수정 (CAPTAIN/MANAGER 권한). 변경할 필드만 채워 보낸다.
func updateTeam(id: Int64, request: UpdateTeamRequest) async throws -> Team
```

- [ ] **Step 2: 같은 파일 끝에 UpdateTeamRequest 구조체 추가**

```swift
/// 팀 정보 수정 요청 데이터 (Domain 계층). 모든 필드 optional — 변경할 필드만 채워서 보낸다.
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
    ) {
        self.name = name
        self.region = region
        self.division = division
        self.activityDays = activityDays
        self.activityTime = activityTime
        self.lineupMode = lineupMode
    }
}
```

- [ ] **Step 3: TeamRequestDTO.swift에 UpdateTeamRequestDTO 추가**

파일 끝(기존 DTO들 다음)에 추가:
```swift
/// PUT /api/v1/teams/{id} 요청 본문.
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

- [ ] **Step 4: TeamRepositoryImpl.updateTeam 구현**

`joinTeam` 메서드 다음에 추가:
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

- [ ] **Step 5: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

---

### Task 4.2: EditTeamViewModel

**Files:**
- Create: `dugout-ios/Features/Team/Sources/Presentation/ViewModels/EditTeamViewModel.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  EditTeamViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class EditTeamViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(Team)
        case failed(String)
    }

    public private(set) var state: State = .editing

    public var name: String
    public var region: String
    public var division: Int
    public var activityDays: Set<DayOfWeek>
    public var activityTime: String
    public var lineupMode: LineupMode

    public let availableDivisions: [Int] = [1, 2, 3, 4]

    private let teamId: Int64
    private let repository: any TeamRepository

    public init(team: Team, repository: any TeamRepository = TeamRepositoryImpl()) {
        self.teamId = team.id
        self.repository = repository
        self.name = team.name
        self.region = team.region
        self.division = team.division
        self.activityDays = Set(team.activityDays.compactMap { DayOfWeek(rawValue: $0) })
        self.activityTime = team.activityTime ?? ""
        self.lineupMode = team.lineupMode
    }

    public var canSubmit: Bool {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedRegion = region.trimmingCharacters(in: .whitespaces)
        if case .submitting = state { return false }
        return !trimmedName.isEmpty && !trimmedRegion.isEmpty
    }

    public func submit() async {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedRegion = region.trimmingCharacters(in: .whitespaces)
        let trimmedTime = activityTime.trimmingCharacters(in: .whitespaces)

        state = .submitting
        let request = UpdateTeamRequest(
            name: trimmedName,
            region: trimmedRegion,
            division: division,
            activityDays: Array(activityDays).map(\.rawValue).sorted(),
            activityTime: trimmedTime.isEmpty ? nil : trimmedTime,
            lineupMode: lineupMode
        )
        do {
            let team = try await repository.updateTeam(id: teamId, request: request)
            state = .success(team)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 정보 수정 중 오류가 발생했습니다")
        }
    }
}
```

---

### Task 4.3: EditTeamView

**Files:**
- Create: `dugout-ios/Features/Team/Sources/Presentation/Views/EditTeamView.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  EditTeamView.swift
//  DugoutTeamFeature
//

import SwiftUI
import DugoutDesignSystem

public struct EditTeamView: View {
    @State private var viewModel: EditTeamViewModel
    private let onCompleted: @MainActor () async -> Void

    @Environment(\.dismiss) private var dismiss

    public init(
        viewModel: EditTeamViewModel,
        onCompleted: @escaping @MainActor () async -> Void
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            Form {
                if case .failed(let message) = viewModel.state {
                    Section {
                        Label(message, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(DGColor.warning)
                    }
                }

                Section("팀 정보") {
                    TextField("팀 이름", text: $viewModel.name)
                    TextField("지역 (예: 서울 강남)", text: $viewModel.region)
                    Picker("부수", selection: $viewModel.division) {
                        ForEach(viewModel.availableDivisions, id: \.self) { div in
                            Text("\(div)부").tag(div)
                        }
                    }
                }

                Section("활동 요일") {
                    ForEach(DayOfWeek.allCases, id: \.self) { day in
                        Toggle(day.displayName, isOn: Binding(
                            get: { viewModel.activityDays.contains(day) },
                            set: { isOn in
                                if isOn { viewModel.activityDays.insert(day) }
                                else { viewModel.activityDays.remove(day) }
                            }
                        ))
                    }
                }

                Section("활동 시간 (선택)") {
                    TextField("예: 18:00-21:00", text: $viewModel.activityTime)
                }

                Section("라인업 모드") {
                    Picker("모드", selection: $viewModel.lineupMode) {
                        Text("균등 출전 (BALANCED)").tag(LineupMode.balanced)
                        Text("실력 우선 (COMPETITIVE)").tag(LineupMode.competitive)
                    }
                    .pickerStyle(.segmented)
                }
            }
            .navigationTitle("팀 정보 수정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소") { dismiss() }
                        .disabled(isSubmitting)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    if isSubmitting {
                        ProgressView()
                    } else {
                        Button("저장") {
                            Task {
                                await viewModel.submit()
                                if case .success = viewModel.state {
                                    await onCompleted()
                                    dismiss()
                                }
                            }
                        }
                        .disabled(!viewModel.canSubmit)
                    }
                }
            }
            .interactiveDismissDisabled(isSubmitting)
        }
    }

    private var isSubmitting: Bool {
        if case .submitting = viewModel.state { return true }
        return false
    }
}
```

---

### Task 4.4: TeamDetailView 편집 진입점 + 빌드 + commit

**Files:**
- Modify: `dugout-ios/Features/Team/Sources/Presentation/Views/TeamDetailView.swift`

- [ ] **Step 1: @State 추가**

`TeamDetailView` struct 안 가장 위 (`@State private var viewModel: TeamDetailViewModel` 줄 다음)에 추가:

```swift
@State private var showEditSheet = false
```

- [ ] **Step 2: body의 modifier chain에 toolbar + sheet 추가**

기존 body의 마지막 modifier(`.task { ... }`) 다음에 추가:

```swift
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

- [ ] **Step 3: Tuist 재생성 + 빌드 검증**

```bash
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: 커밋**

```bash
git add dugout-ios/Features/Team/Sources/Domain/Repositories/TeamRepository.swift \
        dugout-ios/Features/Team/Sources/Data/DTOs/TeamRequestDTO.swift \
        dugout-ios/Features/Team/Sources/Data/Repositories/TeamRepositoryImpl.swift \
        dugout-ios/Features/Team/Sources/Presentation/ViewModels/EditTeamViewModel.swift \
        dugout-ios/Features/Team/Sources/Presentation/Views/EditTeamView.swift \
        dugout-ios/Features/Team/Sources/Presentation/Views/TeamDetailView.swift
git commit -m "feat(ios): 팀 정보 수정 화면 (CAPTAIN/MANAGER)

- TeamRepository.updateTeam + UpdateTeamRequest + DTO
- EditTeamViewModel: 기존 Team 값으로 폼 초기화 → submit 시 PUT /teams/{id}
- EditTeamView: Form + 저장/취소 toolbar + submitting indicator + 에러 상단 노출
- TeamDetailView toolbar에 편집 버튼 (canEditTeam) → sheet"
```

---

## Milestone 5 — 멤버 관리 (역할 변경 / 추방)

### Task 5.1: Repository 메서드 + DTO

**Files:**
- Modify: `dugout-ios/Features/Team/Sources/Domain/Repositories/TeamRepository.swift`
- Modify: `dugout-ios/Features/Team/Sources/Data/DTOs/TeamRequestDTO.swift`
- Modify: `dugout-ios/Features/Team/Sources/Data/Repositories/TeamRepositoryImpl.swift`

- [ ] **Step 1: protocol에 메서드 2개 추가**

`updateTeam` 다음에 추가:
```swift
/// 멤버 역할 변경 (CAPTAIN 전용). 응답으로 최신 TeamMember.
func updateMember(teamId: Int64, memberId: Int64, role: TeamRole) async throws -> TeamMember

/// 멤버 추방 (CAPTAIN 전용). 백엔드는 CAPTAIN 본인 추방 거부.
func removeMember(teamId: Int64, memberId: Int64) async throws
```

- [ ] **Step 2: TeamRequestDTO.swift에 UpdateMemberRequestDTO 추가**

파일 끝(UpdateTeamRequestDTO 다음)에 추가:
```swift
/// PUT /api/v1/teams/{teamId}/members/{memberId} 요청 본문.
/// 이번 plan 범위: role만 변경. jersey_number / positions는 후속 plan.
struct UpdateMemberRequestDTO: Encodable, Sendable {
    let role: String
}
```

- [ ] **Step 3: TeamRepositoryImpl 구현 추가**

`updateTeam` 다음에 추가:
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

- [ ] **Step 4: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

---

### Task 5.2: TeamDetailViewModel 멤버 액션 확장

**Files:**
- Modify: `dugout-ios/Features/Team/Sources/Presentation/ViewModels/TeamDetailViewModel.swift`

- [ ] **Step 1: 새 state 프로퍼티 추가**

기존 invite 관련 프로퍼티(`inviteCode`, `inviteCodeError`, `isGeneratingInviteCode`) 다음에 추가:
```swift
public var selectedMember: TeamMember?
public var memberActionError: String?
public private(set) var isMemberActionInFlight: Bool = false
```

- [ ] **Step 2: 권한/액션 메서드 추가**

기존 마지막 메서드(`generateInviteCode`) 다음에 추가:

```swift
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
        _ = try await repository.updateMember(
            teamId: teamId,
            memberId: member.id,
            role: role
        )
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

> `teamId`, `currentUserId`, `repository`, `canManageMembers`는 ViewModel에 이미 있는 필드/computed. `load()`는 기존 메서드 재사용 (전체 reload 후 UI 갱신).

- [ ] **Step 3: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

---

### Task 5.3: TeamDetailView ConfirmationDialog + alert + MemberRow + commit

**Files:**
- Modify: `dugout-ios/Features/Team/Sources/Presentation/Views/TeamDetailView.swift`

- [ ] **Step 1: 파일 끝에 MemberRow private struct 추가**

`TeamDetailView` struct 닫힌 후, 파일 끝에 다음 추가:

```swift
private struct MemberRow: View {
    let member: TeamMember

    var body: some View {
        DGCard {
            HStack(spacing: DGSpacing.md) {
                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(DGColor.textSecondary)
                VStack(alignment: .leading, spacing: DGSpacing.xs) {
                    HStack {
                        Text(member.nickname)
                            .font(DGFont.callout)
                        if let jersey = member.jerseyNumber {
                            Text("#\(jersey)")
                                .font(DGFont.caption)
                                .foregroundStyle(DGColor.textSecondary)
                        }
                    }
                    HStack(spacing: DGSpacing.xs) {
                        Text(member.role.displayName)
                            .font(DGFont.caption)
                            .padding(.horizontal, DGSpacing.xs)
                            .padding(.vertical, 2)
                            .background(DGColor.primary.opacity(0.1))
                            .foregroundStyle(DGColor.primary)
                            .clipShape(Capsule())
                        if !member.positions.isEmpty {
                            Text(member.positions.joined(separator: ", "))
                                .font(DGFont.caption)
                                .foregroundStyle(DGColor.textSecondary)
                        }
                    }
                }
                Spacer()
            }
            .padding(DGSpacing.sm)
        }
    }
}
```

- [ ] **Step 2: membersSection의 ForEach 본문을 actionable 분기로 교체**

기존 `membersSection(members:)` 안의 `ForEach(members) { member in DGCard { ... } }` 블록을 다음으로 교체:

```swift
ForEach(members) { member in
    if viewModel.isMemberActionable(member) {
        Button {
            viewModel.tapMember(member)
        } label: {
            MemberRow(member: member)
        }
        .buttonStyle(.plain)
    } else {
        MemberRow(member: member)
    }
}
```

- [ ] **Step 3: body modifier chain에 ConfirmationDialog + alert 추가**

기존 body의 마지막 modifier (M4에서 추가한 `.sheet(isPresented: $showEditSheet) { ... }`) 다음에 추가:

```swift
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
} message: { error in
    Text(error)
}
```

- [ ] **Step 4: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | tail -5
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 5: 커밋**

```bash
git add dugout-ios/Features/Team/Sources/Domain/Repositories/TeamRepository.swift \
        dugout-ios/Features/Team/Sources/Data/DTOs/TeamRequestDTO.swift \
        dugout-ios/Features/Team/Sources/Data/Repositories/TeamRepositoryImpl.swift \
        dugout-ios/Features/Team/Sources/Presentation/ViewModels/TeamDetailViewModel.swift \
        dugout-ios/Features/Team/Sources/Presentation/Views/TeamDetailView.swift
git commit -m "feat(ios): 멤버 역할 변경/추방 (CAPTAIN, ConfirmationDialog)

- TeamRepository.updateMember / removeMember + UpdateMemberRequestDTO
- TeamDetailViewModel: selectedMember + 액션 + 에러 상태
- isMemberActionable: canManageMembers + 자기 자신/CAPTAIN 비활성
- MemberRow private struct 추출 (가독성/재사용)
- ConfirmationDialog: 매니저/회계/일반 변경 + 추방(destructive) + 취소
- 에러는 alert로 표시"
```

---

## 6. End-to-End 시뮬레이터 검증 (M5 완료 시점)

### Prerequisites

- 백엔드 띄움: `cd dugout-api && ./gradlew bootRun --args='--spring.profiles.active=local'`
- 시뮬레이터 Erase All Content and Settings (앱 캐시·토큰 초기화)

### 14단계 통합 시나리오

**A. 세션 rehydration (M2)**
1. "주장A"로 dev-login → 팀 "테스트팀" 생성 → 홈에 카드 + 마이페이지에 사용자 정보 표시
2. 앱 강제 종료 (Cmd+Shift+H 두 번 → 위로 swipe)
3. 앱 다시 실행 → splash → **재로그인 없이** 홈에 카드, 마이페이지에 "주장A" 자동 복원

**B. 팀 정보 수정 + CAPTAIN 권한 (M3, M4)**
4. 카드 탭 → TeamDetailView → toolbar **"편집" 버튼 노출** (CAPTAIN)
5. 편집 → EditTeamView 시트 → 이름/지역/부수/활동 요일 변경 → 저장 → 시트 dismiss → TeamDetailView 갱신

**C. 멤버 가입 + MEMBER 권한 (M5, M3)**
6. 초대 코드 생성·메모 → 마이페이지 로그아웃
7. "용병B" 로그인 → 초대 코드로 가입 → 홈 카드 표시
8. 카드 탭 → 멤버 (2) 표시 + **편집 버튼 미표시 + 초대 코드 영역 미표시** (MEMBER)
9. **멤버 row 탭 무반응** (`canManageMembers == false`)

**D. CAPTAIN의 멤버 관리 (M5)**
10. 로그아웃 → "주장A" 재로그인 → 카드 탭 → 용병B row 탭 → ConfirmationDialog → "매니저로 변경" → reload → 배지 "매니저"
11. **본인(주장A) row 탭 무반응** (자기 자신 비활성)

**E. MANAGER 권한 (M3)**
12. 로그아웃 → "용병B" 재로그인 → 카드 탭
    - **편집 버튼 노출** (MANAGER → `canEditTeam`)
    - **초대 코드 영역 노출** (MANAGER → `canShowInviteCode`)
    - **멤버 row 탭 여전히 무반응** (`canManageMembers == false`; CAPTAIN 전용)

**F. 추방 (M5)**
13. 로그아웃 → "주장A" 재로그인 → 카드 탭 → 용병B row 탭 → "추방" → reload → 멤버 (1)
14. 마이페이지 로그아웃 → 비로그인 복귀

---

## 7. 커밋 전 체크리스트

- [ ] 모든 milestone에서 `xcodebuild ... build` BUILD SUCCEEDED
- [ ] Swift 6 Strict Concurrency 경고 없음
- [ ] 백엔드 `./gradlew build -x test` BUILD SUCCESSFUL
- [ ] `/users/me` 응답이 `UserDTO`로 정상 디코딩 (시뮬레이터 검증 step 3)
- [ ] 시뮬레이터 14단계 통과
- [ ] `docs/TDD.md` API 섹션 업데이트 완료

## 8. 후속 작업 (이번 plan 범위 외)

- CAPTAIN 권한 위임 (백엔드 자동 강등 로직 필요)
- 멤버 jersey number / position 수정 (`UpdateMember`의 다른 필드)
- 팀 logo 업로드 (이미지 처리)
- 팀 탈퇴 (member 본인이 deactivate)
- TDD.md iOS 아키텍처 섹션 정비 (별도 D 작업)
