# Team Feature MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** iOS에서 팀 생성·가입·상세까지 동작하는 end-to-end 흐름을 완성하고, 메인 탭 셸 + Deferred Auth 패턴을 도입한다.

**Architecture:** Tuist 기반 모듈식 구조 (App + Core/Network + Core/DesignSystem + Features/Auth, Home, Team). 인증 무관 진입 후 필요한 액션(팀 생성/가입) 시 LoginSheet 트리거. 기존 `AuthViewModel`을 환경 객체로 모든 feature가 공유.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, @Observable, Tuist 4.x, Alamofire, Spring Boot backend (`dugout-api`)

---

## 0. 사전 준비 — 코드 베이스 현황 (PLAN ONLY, NOT A TASK)

이 plan은 spec 작성 시점의 가정과 다른 다음 사실들을 반영한다.

### 이미 구현된 것 (재사용)

- `DugoutHomeFeature` — `MyTeam` Entity, `MyTeamDTO`, `HomeRepository`, `HomeRepositoryImpl`, `HomeViewModel`, `HomeView` 전부 존재
- `DugoutAuthFeature` — `AuthViewModel`에 `devLogin(nickname:)`, `login(provider:accessToken:)`, `logout()`, `checkAuthStatus()` 모두 구현
- `DugoutDesignSystem` — `DGCard`, `DGButton`, `DGColor`, `DGSpacing`, `DGFont` 사용 가능 (기존 HomeView에서 활용 중)
- `DugoutTeamFeature` Domain/Data — `Team`, `TeamMember`, `TeamRepository`, `TeamRepositoryImpl`, DTO 모두 존재
- `DugoutCoreNetwork` — `APIClient`, `AuthInterceptor`(401 자동 refresh), `TokenStore` 모두 동작

### 신규로 만들 것

- `App/Sources/SplashView.swift`
- `App/Sources/MainTabView.swift`
- `App/Sources/MyPageView.swift` (단순 뷰 → App 모듈 안에 둠, Profile 모듈 별도 생성 X)
- `Features/Auth/Sources/Presentation/Views/LoginSheet.swift`
- `Features/Team/Sources/Presentation/Views/CreateTeam/{CreateTeamViewModel, CreateTeamView}.swift`
- `Features/Team/Sources/Presentation/Views/JoinTeam/{JoinTeamViewModel, JoinTeamView}.swift`
- `Features/Team/Sources/Presentation/Views/TeamDetail/{TeamDetailViewModel, TeamDetailView}.swift`

### 수정할 것

- `App/Sources/DugoutApp.swift` — `RootView` 분기 제거, `SplashView` → `MainTabView` 직진
- `App/Sources/RootView.swift` — 삭제
- `Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift` — `isAuthenticated`, `currentUser` computed property 추가
- `Features/Home/Sources/Presentation/ViewModels/HomeViewModel.swift` — Deferred auth 트리거(`pendingAction`, `presentedSheet`) 추가
- `Features/Home/Sources/Presentation/Views/HomeView.swift` — 비로그인 분기, sheet 연결, 팀 카드 탭 → 상세 push

### 빌드/검증 명령

```bash
# 프로젝트 디렉토리
cd /Users/heetae/Documents/Source/Dugout/dugout-ios

# 모듈 변경 시 (Tuist 갱신)
tuist generate

# 빌드 검증
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build

# 시뮬레이터 실행 (수동 검증)
open Dugout.xcworkspace
# Xcode에서 Cmd+R로 시뮬레이터 실행
```

> 단위 테스트 타겟이 없으므로 각 task의 검증은 **빌드 성공 + 시뮬레이터 동작 확인**으로 한다.

---

## Milestone 1 — 메인 탭 셸 + 진입 흐름 변경

### Task 1.1: SplashView 생성

**Files:**
- Create: `dugout-ios/App/Sources/SplashView.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  SplashView.swift
//  Dugout
//

import SwiftUI
import DugoutDesignSystem

struct SplashView: View {
    let onReady: @MainActor () -> Void

    var body: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "baseball.diamond.bases")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.primary)
            Text("Dugout")
                .font(DGFont.largeTitle)
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

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

- [ ] **Step 3: 커밋**

```bash
git add dugout-ios/App/Sources/SplashView.swift
git commit -m "feat(ios): SplashView 추가 (1.2초 자동 전환)"
```

---

### Task 1.2: MainTabView 골격 생성

**Files:**
- Create: `dugout-ios/App/Sources/MainTabView.swift`

- [ ] **Step 1: placeholder 포함한 골격 작성**

```swift
//
//  MainTabView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutHomeFeature

struct MainTabView: View {
    @Bindable var authViewModel: AuthViewModel

    var body: some View {
        TabView {
            HomeView(authViewModel: authViewModel)
                .tabItem {
                    Label("홈", systemImage: "house.fill")
                }

            MyPageView(authViewModel: authViewModel)
                .tabItem {
                    Label("마이페이지", systemImage: "person.crop.circle")
                }
        }
    }
}
```

> 이 시점에서는 `HomeView(authViewModel:)`와 `MyPageView`가 아직 변경되지 않아 컴파일이 깨진다. Task 1.5 직후 함께 검증한다.

- [ ] **Step 2: 다음 task로 이동 (빌드 검증은 1.5 이후)**

---

### Task 1.3: MyPageView placeholder 생성

**Files:**
- Create: `dugout-ios/App/Sources/MyPageView.swift`

- [ ] **Step 1: placeholder 작성 (M2에서 채움)**

```swift
//
//  MyPageView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutDesignSystem

struct MyPageView: View {
    @Bindable var authViewModel: AuthViewModel

    var body: some View {
        NavigationStack {
            VStack(spacing: DGSpacing.lg) {
                Text("마이페이지")
                    .font(DGFont.title2)
                Text("M2에서 구현")
                    .font(DGFont.callout)
                    .foregroundStyle(DGColor.textSecondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(DGColor.background)
            .navigationTitle("마이페이지")
        }
    }
}
```

---

### Task 1.4: HomeView 시그니처 변경 (authViewModel 받도록)

**Files:**
- Modify: `dugout-ios/Features/Home/Sources/Presentation/Views/HomeView.swift`

> 기존 HomeView는 `onLogout: () -> Void` 클로저를 받는다. 메인 탭 안에서는 logout이 마이페이지에서 처리되므로 콜백 시그니처를 제거하고 `AuthViewModel`을 직접 받도록 변경한다.

- [ ] **Step 1: HomeView 시그니처 변경**

기존 코드:
```swift
public struct HomeView: View {
    @State private var viewModel: HomeViewModel
    private let onLogout: () -> Void

    public init(
        viewModel: HomeViewModel = HomeViewModel(),
        onLogout: @escaping () -> Void
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.onLogout = onLogout
    }
```

변경 후:
```swift
import DugoutAuthFeature

public struct HomeView: View {
    @State private var viewModel: HomeViewModel
    @Bindable var authViewModel: AuthViewModel

    public init(
        viewModel: HomeViewModel = HomeViewModel(),
        authViewModel: AuthViewModel
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.authViewModel = authViewModel
    }
```

- [ ] **Step 2: toolbar 메뉴에서 "로그아웃" 항목 제거**

기존 코드 (45–48 line):
```swift
Button("로그아웃", role: .destructive) {
    onLogout()
}
```

위 3줄을 삭제 (이 시점에서 toolbar Menu에 "새로고침"만 남음).

> Tuist Project.swift에서 `homeFeature`의 dependencies에 `DugoutAuthFeature` 추가 필요.

- [ ] **Step 3: Tuist Project.swift 수정 — Home이 Auth 의존하도록**

`dugout-ios/Project.swift` line 61–68:

기존:
```swift
let homeFeature = frameworkTarget(
    name: "DugoutHomeFeature",
    sourcesPath: "Features/Home/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
    ]
)
```

변경:
```swift
let homeFeature = frameworkTarget(
    name: "DugoutHomeFeature",
    sourcesPath: "Features/Home/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
        .target(name: "DugoutAuthFeature"),
    ]
)
```

- [ ] **Step 4: Tuist 재생성**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
```

Expected: 정상 종료 (`Project generated successfully`)

---

### Task 1.5: DugoutApp 진입점 변경 + RootView 삭제

**Files:**
- Modify: `dugout-ios/App/Sources/DugoutApp.swift`
- Delete: `dugout-ios/App/Sources/RootView.swift`

- [ ] **Step 1: DugoutApp 변경**

기존:
```swift
@main
struct DugoutApp: App {
    @State private var authViewModel = AuthViewModel()

    var body: some Scene {
        WindowGroup {
            RootView(authViewModel: authViewModel)
                .task {
                    await authViewModel.checkAuthStatus()
                }
        }
    }
}
```

변경:
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

- [ ] **Step 2: RootView 파일 삭제**

```bash
rm /Users/heetae/Documents/Source/Dugout/dugout-ios/App/Sources/RootView.swift
```

- [ ] **Step 3: Tuist 재생성**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
```

- [ ] **Step 4: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

- [ ] **Step 5: 시뮬레이터 동작 확인 (수동)**

```bash
open Dugout.xcworkspace
# Xcode에서 Cmd+R
```

확인 항목:
- 앱 실행 → 스플래시 화면(야구장 아이콘 + Dugout) 1.2초 표시
- 자동으로 메인 탭 (홈 / 마이페이지) 진입
- 홈 탭은 기존 동작 (인증 안 된 상태면 401 → 새로고침/실패 상태 노출 가능)
- 마이페이지 탭은 "M2에서 구현" placeholder 표시

- [ ] **Step 6: 커밋**

```bash
git add dugout-ios/App/Sources/ dugout-ios/Features/Home/Sources/Presentation/Views/HomeView.swift dugout-ios/Project.swift
git commit -m "feat(ios): 메인 탭 셸 도입 + Splash 진입

- SplashView (1.2초 자동 전환)
- MainTabView (홈/마이페이지)
- RootView 제거, DugoutApp이 직접 분기
- HomeView가 onLogout 대신 AuthViewModel 직접 수신
- HomeFeature가 AuthFeature 의존 추가"
```

---

## Milestone 2 — 인증 통합 + 마이페이지 + LoginSheet

### Task 2.1: AuthViewModel에 편의 property 추가

**Files:**
- Modify: `dugout-ios/Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift`

- [ ] **Step 1: `isAuthenticated`, `currentUser` computed property 추가**

기존 `state` 프로퍼티 바로 아래(line 21 이후)에 추가:

```swift
public var isAuthenticated: Bool {
    if case .authenticated = state { return true }
    return false
}

public var currentUser: User? {
    if case .authenticated(let user) = state { return user }
    return nil
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

- [ ] **Step 3: 커밋**

```bash
git add dugout-ios/Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift
git commit -m "feat(ios): AuthViewModel.isAuthenticated/currentUser computed property 추가"
```

---

### Task 2.2: LoginSheet 컴포넌트 생성

**Files:**
- Create: `dugout-ios/Features/Auth/Sources/Presentation/Views/LoginSheet.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  LoginSheet.swift
//  DugoutAuthFeature
//

import SwiftUI
import DugoutDesignSystem

/// 인증이 필요한 액션을 시도할 때 표시되는 공용 로그인 시트.
/// dev-login만 지원 (Phase 1). OAuth는 후속.
public struct LoginSheet: View {
    @Bindable var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var nickname: String = ""

    public init(authViewModel: AuthViewModel) {
        self.authViewModel = authViewModel
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: DGSpacing.lg) {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.system(size: 60))
                    .foregroundStyle(DGColor.primary)
                    .padding(.top, DGSpacing.xl)

                Text("로그인이 필요합니다")
                    .font(DGFont.title2)

                Text("개발 모드에서는 닉네임만 입력하면 로그인됩니다.")
                    .font(DGFont.footnote)
                    .foregroundStyle(DGColor.textSecondary)
                    .multilineTextAlignment(.center)

                TextField("닉네임", text: $nickname)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal, DGSpacing.lg)
                    .disabled(isLoading)

                if case .failed(let message) = authViewModel.state {
                    Text(message)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.warning)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, DGSpacing.lg)
                }

                DGButton(isLoading ? "로그인 중..." : "로그인") {
                    Task {
                        await authViewModel.devLogin(nickname: nickname.trimmingCharacters(in: .whitespaces))
                    }
                }
                .disabled(nickname.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
                .padding(.horizontal, DGSpacing.lg)

                Spacer()
            }
            .background(DGColor.background)
            .navigationTitle("로그인")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
            .onChange(of: authViewModel.isAuthenticated) { _, isAuth in
                if isAuth { dismiss() }
            }
        }
    }

    private var isLoading: Bool {
        if case .loading = authViewModel.state { return true }
        return false
    }
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

- [ ] **Step 3: 커밋**

```bash
git add dugout-ios/Features/Auth/Sources/Presentation/Views/LoginSheet.swift
git commit -m "feat(ios): 공용 LoginSheet 추가 (dev-login)"
```

---

### Task 2.3: MyPageView 채우기 (비로그인/로그인 분기)

**Files:**
- Modify: `dugout-ios/App/Sources/MyPageView.swift`

- [ ] **Step 1: 전체 교체**

```swift
//
//  MyPageView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutDesignSystem

struct MyPageView: View {
    @Bindable var authViewModel: AuthViewModel
    @State private var showLoginSheet = false

    var body: some View {
        NavigationStack {
            Group {
                if authViewModel.isAuthenticated, let user = authViewModel.currentUser {
                    authenticatedContent(user: user)
                } else {
                    guestContent
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(DGColor.background)
            .navigationTitle("마이페이지")
        }
        .sheet(isPresented: $showLoginSheet) {
            LoginSheet(authViewModel: authViewModel)
        }
    }

    @ViewBuilder
    private var guestContent: some View {
        VStack(spacing: DGSpacing.lg) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.textSecondary)

            Text("로그인이 필요합니다")
                .font(DGFont.title3)

            Text("팀 활동을 시작하려면 로그인하세요")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)

            DGButton("로그인") {
                showLoginSheet = true
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.top, DGSpacing.md)
        }
    }

    @ViewBuilder
    private func authenticatedContent(user: User) -> some View {
        VStack(spacing: DGSpacing.lg) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.primary)

            VStack(spacing: DGSpacing.xs) {
                Text(user.nickname)
                    .font(DGFont.title2)
                if let email = user.email {
                    Text(email)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.textSecondary)
                }
            }

            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.sm) {
                    HStack {
                        Text("로그인 방식")
                            .foregroundStyle(DGColor.textSecondary)
                        Spacer()
                        Text(user.provider)
                            .font(DGFont.callout)
                    }
                }
                .padding(DGSpacing.sm)
            }
            .padding(.horizontal, DGSpacing.lg)

            Spacer()

            DGButton("로그아웃") {
                Task { await authViewModel.logout() }
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.bottom, DGSpacing.xl)
        }
        .padding(.top, DGSpacing.xl)
    }
}
```

> `User` Entity의 정확한 필드는 `Features/Auth/Sources/Domain/Entities/User.swift`를 참고. `nickname`, `email`, `provider` 등이 있는지 확인 후 필요시 표시 항목 조정.

- [ ] **Step 2: User Entity 필드 확인**

```bash
cat /Users/heetae/Documents/Source/Dugout/dugout-ios/Features/Auth/Sources/Domain/Entities/User.swift
```

위 코드에서 사용한 필드(`nickname`, `email`, `provider`)가 모두 있는지 확인. 없는 필드는 코드에서 제거하거나 default 값 사용.

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

- [ ] **Step 4: 시뮬레이터 검증**

확인 항목:
- 앱 실행 → 스플래시 → 메인 탭
- 마이페이지 탭 → "로그인이 필요합니다" 안내 + [로그인] 버튼
- [로그인] 탭 → LoginSheet 표시 → 닉네임 입력 → 로그인
- 로그인 성공 → 시트 자동 dismiss → 마이페이지에 사용자 정보 + 로그아웃 버튼
- [로그아웃] 탭 → 비로그인 상태 복귀 ("로그인이 필요합니다" 다시 표시)

- [ ] **Step 5: 커밋**

```bash
git add dugout-ios/App/Sources/MyPageView.swift
git commit -m "feat(ios): 마이페이지 비로그인/로그인 분기 + LoginSheet 연동"
```

---

## Milestone 3 — 홈 비로그인 분기 + 팀 생성/가입 (Deferred Auth)

### Task 3.1: HomeViewModel — Deferred Auth 트리거 상태 추가

**Files:**
- Modify: `dugout-ios/Features/Home/Sources/Presentation/ViewModels/HomeViewModel.swift`

- [ ] **Step 1: 전체 교체**

```swift
//
//  HomeViewModel.swift
//  DugoutHomeFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class HomeViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded([MyTeam])
        case failed(String)
    }

    public enum PendingAction: Sendable {
        case createTeam
        case joinTeam
    }

    public enum PresentedSheet: Identifiable, Sendable {
        case createTeam
        case joinTeam
        case login

        public var id: String {
            switch self {
            case .createTeam: "createTeam"
            case .joinTeam: "joinTeam"
            case .login: "login"
            }
        }
    }

    public private(set) var state: State = .idle
    public var presentedSheet: PresentedSheet?
    public var pendingAction: PendingAction?

    private let repository: any HomeRepository

    public init(repository: any HomeRepository = HomeRepositoryImpl()) {
        self.repository = repository
    }

    public func loadTeams() async {
        state = .loading
        do {
            let teams = try await repository.fetchMyTeams()
            state = .loaded(teams)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 목록을 불러오지 못했습니다")
        }
    }

    /// 팀 만들기 액션 트리거. 비로그인이면 LoginSheet 띄우고 pending에 보관.
    public func tapCreateTeam(isAuthenticated: Bool) {
        if isAuthenticated {
            presentedSheet = .createTeam
        } else {
            pendingAction = .createTeam
            presentedSheet = .login
        }
    }

    /// 팀 가입 액션 트리거. 비로그인이면 LoginSheet 띄우고 pending에 보관.
    public func tapJoinTeam(isAuthenticated: Bool) {
        if isAuthenticated {
            presentedSheet = .joinTeam
        } else {
            pendingAction = .joinTeam
            presentedSheet = .login
        }
    }

    /// 인증 상태 변화 감지 시 호출. 로그인 직후 pending action을 이어간다.
    public func onAuthChanged(isAuthenticated: Bool) {
        guard isAuthenticated, let action = pendingAction else { return }
        switch action {
        case .createTeam: presentedSheet = .createTeam
        case .joinTeam: presentedSheet = .joinTeam
        }
        pendingAction = nil
    }

    /// 팀 생성/가입 완료 후 호출 — 시트 닫고 목록 새로고침.
    public func onTeamMutated() async {
        presentedSheet = nil
        await loadTeams()
    }
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED (HomeView가 깨질 수 있음 → 다음 task에서 함께 검증)

- [ ] **Step 3: HomeView가 깨지면 임시 주석으로 통과**

빌드 실패 시 HomeView의 toolbar 부분을 임시 주석 처리. Task 3.2에서 재작성.

- [ ] **Step 4: 임시 커밋 X — Task 3.2와 함께 커밋**

---

### Task 3.2: CreateTeamViewModel + CreateTeamView 생성

**Files:**
- Create: `dugout-ios/Features/Team/Sources/Presentation/ViewModels/CreateTeamViewModel.swift`
- Create: `dugout-ios/Features/Team/Sources/Presentation/Views/CreateTeamView.swift`

- [ ] **Step 1: CreateTeamViewModel 작성**

```swift
//
//  CreateTeamViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class CreateTeamViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(Team)
        case failed(String)
    }

    public private(set) var state: State = .editing

    public var name: String = ""
    public var region: String = ""
    public var division: Int = 4
    public var activityDays: Set<String> = []
    public var activityTime: String = ""
    public var lineupMode: LineupMode = .balanced

    public let availableDays: [String] = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
    public let availableDivisions: [Int] = [1, 2, 3, 4]

    private let repository: any TeamRepository

    public init(repository: any TeamRepository = TeamRepositoryImpl()) {
        self.repository = repository
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
        let request = CreateTeamRequest(
            name: trimmedName,
            region: trimmedRegion,
            division: division,
            activityDays: Array(activityDays).sorted(),
            activityTime: trimmedTime.isEmpty ? nil : trimmedTime,
            lineupMode: lineupMode
        )
        do {
            let team = try await repository.createTeam(request)
            state = .success(team)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 생성 중 오류가 발생했습니다")
        }
    }
}
```

- [ ] **Step 2: CreateTeamView 작성**

```swift
//
//  CreateTeamView.swift
//  DugoutTeamFeature
//

import SwiftUI
import DugoutDesignSystem

public struct CreateTeamView: View {
    @State private var viewModel: CreateTeamViewModel
    private let onCompleted: @MainActor () async -> Void

    @Environment(\.dismiss) private var dismiss

    public init(
        viewModel: CreateTeamViewModel = CreateTeamViewModel(),
        onCompleted: @escaping @MainActor () async -> Void
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            Form {
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
                    ForEach(viewModel.availableDays, id: \.self) { day in
                        Toggle(displayDay(day), isOn: Binding(
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

                if case .failed(let message) = viewModel.state {
                    Section {
                        Text(message)
                            .foregroundStyle(DGColor.warning)
                    }
                }
            }
            .navigationTitle("팀 만들기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("생성") {
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
            .interactiveDismissDisabled(isSubmitting)
        }
    }

    private var isSubmitting: Bool {
        if case .submitting = viewModel.state { return true }
        return false
    }

    private func displayDay(_ code: String) -> String {
        switch code {
        case "MON": "월"
        case "TUE": "화"
        case "WED": "수"
        case "THU": "목"
        case "FRI": "금"
        case "SAT": "토"
        case "SUN": "일"
        default: code
        }
    }
}
```

> 디렉토리 `Features/Team/Sources/Presentation/ViewModels/` 와 `Features/Team/Sources/Presentation/Views/` 가 없으면 새로 생성됨 (Tuist `sources: "Features/Team/Sources/**"` 가 자동 인식).

- [ ] **Step 3: 빌드 검증 (3.1과 함께)**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

> 만약 `LineupMode` 가 public 이 아니어서 노출 안 되면 Domain Entity 확인 후 수정. 아래 Task 3.5와 함께 처리.

---

### Task 3.3: JoinTeamViewModel + JoinTeamView 생성

**Files:**
- Create: `dugout-ios/Features/Team/Sources/Presentation/ViewModels/JoinTeamViewModel.swift`
- Create: `dugout-ios/Features/Team/Sources/Presentation/Views/JoinTeamView.swift`

- [ ] **Step 1: JoinTeamViewModel 작성**

```swift
//
//  JoinTeamViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class JoinTeamViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(TeamMember)
        case failed(String)
    }

    public private(set) var state: State = .editing
    public var inviteCode: String = ""

    private let repository: any TeamRepository

    public init(repository: any TeamRepository = TeamRepositoryImpl()) {
        self.repository = repository
    }

    public var canSubmit: Bool {
        let trimmed = inviteCode.trimmingCharacters(in: .whitespaces)
        if case .submitting = state { return false }
        return !trimmed.isEmpty
    }

    public func submit() async {
        let trimmed = inviteCode.trimmingCharacters(in: .whitespaces)
        state = .submitting
        do {
            let member = try await repository.joinTeam(inviteCode: trimmed)
            state = .success(member)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 가입 중 오류가 발생했습니다")
        }
    }
}
```

- [ ] **Step 2: JoinTeamView 작성**

```swift
//
//  JoinTeamView.swift
//  DugoutTeamFeature
//

import SwiftUI
import DugoutDesignSystem

public struct JoinTeamView: View {
    @State private var viewModel: JoinTeamViewModel
    private let onCompleted: @MainActor () async -> Void

    @Environment(\.dismiss) private var dismiss

    public init(
        viewModel: JoinTeamViewModel = JoinTeamViewModel(),
        onCompleted: @escaping @MainActor () async -> Void
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: DGSpacing.lg) {
                Image(systemName: "ticket")
                    .font(.system(size: 60))
                    .foregroundStyle(DGColor.primary)
                    .padding(.top, DGSpacing.xl)

                Text("초대 코드 입력")
                    .font(DGFont.title2)

                Text("팀 주장이 공유한 초대 코드를 입력하세요.")
                    .font(DGFont.footnote)
                    .foregroundStyle(DGColor.textSecondary)
                    .multilineTextAlignment(.center)

                TextField("초대 코드", text: $viewModel.inviteCode)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .padding(.horizontal, DGSpacing.lg)
                    .disabled(isSubmitting)

                if case .failed(let message) = viewModel.state {
                    Text(message)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.warning)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, DGSpacing.lg)
                }

                DGButton(isSubmitting ? "가입 중..." : "가입하기") {
                    Task {
                        await viewModel.submit()
                        if case .success = viewModel.state {
                            await onCompleted()
                            dismiss()
                        }
                    }
                }
                .disabled(!viewModel.canSubmit)
                .padding(.horizontal, DGSpacing.lg)

                Spacer()
            }
            .background(DGColor.background)
            .navigationTitle("팀 가입")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
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

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

---

### Task 3.4: HomeView 비로그인 분기 + sheet 연결

**Files:**
- Modify: `dugout-ios/Features/Home/Sources/Presentation/Views/HomeView.swift`
- Modify: `dugout-ios/Project.swift` (Home에 Team 의존 추가)

- [ ] **Step 1: Project.swift 수정 — Home이 Team 의존**

기존 (Task 1.4 이후):
```swift
let homeFeature = frameworkTarget(
    name: "DugoutHomeFeature",
    sourcesPath: "Features/Home/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
        .target(name: "DugoutAuthFeature"),
    ]
)
```

변경:
```swift
let homeFeature = frameworkTarget(
    name: "DugoutHomeFeature",
    sourcesPath: "Features/Home/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
        .target(name: "DugoutAuthFeature"),
        .target(name: "DugoutTeamFeature"),
    ]
)
```

- [ ] **Step 2: HomeView 전체 교체**

```swift
//
//  HomeView.swift
//  DugoutHomeFeature
//

import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature
import DugoutTeamFeature

public struct HomeView: View {
    @State private var viewModel: HomeViewModel
    @Bindable var authViewModel: AuthViewModel

    public init(
        viewModel: HomeViewModel = HomeViewModel(),
        authViewModel: AuthViewModel
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.authViewModel = authViewModel
    }

    public var body: some View {
        NavigationStack {
            content
                .background(DGColor.background)
                .navigationTitle("내 팀")
                .toolbar { toolbarContent }
        }
        .task {
            if authViewModel.isAuthenticated {
                await viewModel.loadTeams()
            }
        }
        .onChange(of: authViewModel.isAuthenticated) { _, isAuth in
            viewModel.onAuthChanged(isAuthenticated: isAuth)
            if isAuth {
                Task { await viewModel.loadTeams() }
            }
        }
        .sheet(item: $viewModel.presentedSheet) { sheet in
            switch sheet {
            case .createTeam:
                CreateTeamView { await viewModel.onTeamMutated() }
            case .joinTeam:
                JoinTeamView { await viewModel.onTeamMutated() }
            case .login:
                LoginSheet(authViewModel: authViewModel)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if !authViewModel.isAuthenticated {
            guestContent
        } else {
            switch viewModel.state {
            case .idle, .loading:
                ProgressView("불러오는 중...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded(let teams):
                if teams.isEmpty {
                    emptyState
                } else {
                    teamList(teams: teams)
                }
            case .failed(let message):
                failedState(message: message)
            }
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        if authViewModel.isAuthenticated {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("새로고침") {
                        Task { await viewModel.loadTeams() }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
    }

    @ViewBuilder
    private var guestContent: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "person.3")
                .font(.system(size: 56))
                .foregroundStyle(DGColor.textSecondary)
            Text("아직 소속된 팀이 없어요")
                .font(DGFont.headline)
            Text("팀을 만들거나 초대 코드로 시작해보세요")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)

            VStack(spacing: DGSpacing.sm) {
                DGButton("팀 만들기") {
                    viewModel.tapCreateTeam(isAuthenticated: authViewModel.isAuthenticated)
                }
                DGButton("팀 가입하기") {
                    viewModel.tapJoinTeam(isAuthenticated: authViewModel.isAuthenticated)
                }
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.top, DGSpacing.lg)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func teamList(teams: [MyTeam]) -> some View {
        ScrollView {
            VStack(spacing: DGSpacing.md) {
                ForEach(teams) { team in
                    DGCard {
                        VStack(alignment: .leading, spacing: DGSpacing.sm) {
                            HStack {
                                Text(team.teamName)
                                    .font(DGFont.title3)
                                Spacer()
                                roleBadge(team.role)
                            }
                            Text("가입일: \(team.joinedAt.formatted(date: .abbreviated, time: .omitted))")
                                .font(DGFont.footnote)
                                .foregroundStyle(DGColor.textSecondary)
                        }
                    }
                }

                actionButtons
            }
            .padding(DGSpacing.lg)
        }
    }

    private var actionButtons: some View {
        VStack(spacing: DGSpacing.sm) {
            DGButton("팀 만들기") {
                viewModel.tapCreateTeam(isAuthenticated: authViewModel.isAuthenticated)
            }
            DGButton("팀 가입하기") {
                viewModel.tapJoinTeam(isAuthenticated: authViewModel.isAuthenticated)
            }
        }
        .padding(.top, DGSpacing.lg)
    }

    private func roleBadge(_ role: TeamRole) -> some View {
        Text(role.displayName)
            .font(DGFont.caption)
            .padding(.horizontal, DGSpacing.sm)
            .padding(.vertical, DGSpacing.xs)
            .background(DGColor.primary.opacity(0.1))
            .foregroundStyle(DGColor.primary)
            .clipShape(Capsule())
    }

    private var emptyState: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "person.3")
                .font(.system(size: 48))
                .foregroundStyle(DGColor.textSecondary)
            Text("아직 소속된 팀이 없어요")
                .font(DGFont.headline)
            Text("팀을 만들거나 초대 코드로 시작해보세요")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)

            VStack(spacing: DGSpacing.sm) {
                DGButton("팀 만들기") {
                    viewModel.tapCreateTeam(isAuthenticated: authViewModel.isAuthenticated)
                }
                DGButton("팀 가입하기") {
                    viewModel.tapJoinTeam(isAuthenticated: authViewModel.isAuthenticated)
                }
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.top, DGSpacing.lg)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func failedState(message: String) -> some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundStyle(DGColor.warning)
            Text(message)
                .font(DGFont.callout)
                .multilineTextAlignment(.center)
                .padding(.horizontal, DGSpacing.xl)
            DGButton("다시 시도") {
                Task { await viewModel.loadTeams() }
            }
            .padding(.horizontal, DGSpacing.xl)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
```

- [ ] **Step 3: Tuist 재생성 + 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

만약 `LineupMode`나 `Team` 등이 import 안 되면 Domain entity의 public 접근 제어 확인 후 수정.

- [ ] **Step 4: 시뮬레이터 검증**

확인 항목:
- 비로그인 상태로 앱 실행 → 홈 탭에 "아직 소속된 팀이 없어요" + [팀 만들기] [팀 가입하기] 버튼
- [팀 만들기] 탭 → LoginSheet 표시 (deferred auth)
- 로그인 → 시트 dismiss → 자동으로 CreateTeamView 표시 (sheet)
- 폼 입력 → "생성" → API 호출 성공 → 시트 dismiss → 홈에 새 팀 표시

- [ ] **Step 5: 커밋**

```bash
git add dugout-ios/Features/ dugout-ios/Project.swift
git commit -m "feat(ios): 홈 비로그인 분기 + 팀 생성/가입 화면 + Deferred Auth

- HomeViewModel에 PendingAction/PresentedSheet 트리거 추가
- HomeView가 비로그인 시 안내, 로그인 시 목록 표시
- CreateTeamView: 폼 입력 → POST /api/v1/teams
- JoinTeamView: 초대 코드 → POST /api/v1/teams/join
- LoginSheet 자동 dismiss 후 pending action 이어가기"
```

---

## Milestone 4 — 팀 상세 + 멤버 목록 + 초대 코드

### Task 4.1: TeamDetailViewModel 생성

**Files:**
- Create: `dugout-ios/Features/Team/Sources/Presentation/ViewModels/TeamDetailViewModel.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamDetailViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class TeamDetailViewModel {
    public struct LoadedData: Sendable, Equatable {
        public let team: Team
        public let members: [TeamMember]
    }

    public enum State: Sendable {
        case idle
        case loading
        case loaded(LoadedData)
        case failed(String)
    }

    public private(set) var state: State = .idle
    public private(set) var inviteCode: String?
    public private(set) var inviteCodeError: String?
    public private(set) var isGeneratingInviteCode: Bool = false

    private let teamId: Int64
    private let currentUserId: Int64?
    private let repository: any TeamRepository

    public init(
        teamId: Int64,
        currentUserId: Int64?,
        repository: any TeamRepository = TeamRepositoryImpl()
    ) {
        self.teamId = teamId
        self.currentUserId = currentUserId
        self.repository = repository
    }

    /// 현재 사용자의 팀 내 역할. 비로그인이거나 멤버가 아니면 nil.
    public var myRole: TeamRole? {
        guard
            case .loaded(let data) = state,
            let userId = currentUserId,
            let me = data.members.first(where: { $0.userId == userId })
        else { return nil }
        return me.role
    }

    public var canShowInviteCode: Bool {
        myRole == .captain
    }

    public func load() async {
        state = .loading
        async let teamTask = repository.fetchTeam(id: teamId)
        async let membersTask = repository.fetchMembers(teamId: teamId)
        do {
            let (team, members) = try await (teamTask, membersTask)
            state = .loaded(LoadedData(team: team, members: members))
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 정보를 불러오지 못했습니다")
        }
    }

    public func generateInviteCode() async {
        isGeneratingInviteCode = true
        inviteCodeError = nil
        defer { isGeneratingInviteCode = false }
        do {
            let code = try await repository.generateInviteCode(teamId: teamId)
            inviteCode = code
        } catch let error as APIError {
            inviteCodeError = error.userMessage
        } catch {
            inviteCodeError = "초대 코드 생성에 실패했습니다"
        }
    }
}
```

> `TeamMember`의 `userId` 필드를 활용해 현재 사용자 매칭. `currentUserId`는 `AuthViewModel.currentUser?.id`로 전달받음 (`User`에 `id: Int64` 가 있다고 가정. 다르면 Auth 도메인 entity 확인 후 수정).

---

### Task 4.2: TeamDetailView 생성

**Files:**
- Create: `dugout-ios/Features/Team/Sources/Presentation/Views/TeamDetailView.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamDetailView.swift
//  DugoutTeamFeature
//

import SwiftUI
import DugoutDesignSystem
import UIKit

public struct TeamDetailView: View {
    @State private var viewModel: TeamDetailViewModel

    public init(viewModel: TeamDetailViewModel) {
        _viewModel = State(wrappedValue: viewModel)
    }

    public var body: some View {
        Group {
            switch viewModel.state {
            case .idle, .loading:
                ProgressView("불러오는 중...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded(let data):
                ScrollView {
                    VStack(alignment: .leading, spacing: DGSpacing.lg) {
                        teamSection(team: data.team)
                        if viewModel.canShowInviteCode {
                            inviteCodeSection
                        }
                        membersSection(members: data.members)
                    }
                    .padding(DGSpacing.lg)
                }
            case .failed(let message):
                VStack(spacing: DGSpacing.md) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 40))
                        .foregroundStyle(DGColor.warning)
                    Text(message)
                        .multilineTextAlignment(.center)
                    DGButton("다시 시도") {
                        Task { await viewModel.load() }
                    }
                    .padding(.horizontal, DGSpacing.xl)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DGColor.background)
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if case .idle = viewModel.state {
                await viewModel.load()
            }
        }
    }

    private var navigationTitle: String {
        if case .loaded(let data) = viewModel.state {
            return data.team.name
        }
        return "팀 상세"
    }

    private func teamSection(team: Team) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                Text(team.name)
                    .font(DGFont.title2)
                infoRow(label: "지역", value: team.region)
                infoRow(label: "부수", value: "\(team.division)부")
                if !team.activityDays.isEmpty {
                    infoRow(label: "활동 요일", value: team.activityDays.map(displayDay).joined(separator: ", "))
                }
                if let time = team.activityTime, !time.isEmpty {
                    infoRow(label: "활동 시간", value: time)
                }
            }
            .padding(DGSpacing.sm)
        }
    }

    private var inviteCodeSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                Text("초대 코드")
                    .font(DGFont.headline)

                if let code = viewModel.inviteCode {
                    HStack {
                        Text(code)
                            .font(DGFont.title3)
                            .monospaced()
                        Spacer()
                        Button {
                            UIPasteboard.general.string = code
                        } label: {
                            Image(systemName: "doc.on.doc")
                        }
                    }
                } else if viewModel.isGeneratingInviteCode {
                    ProgressView("생성 중...")
                } else {
                    DGButton("초대 코드 생성") {
                        Task { await viewModel.generateInviteCode() }
                    }
                }

                if let error = viewModel.inviteCodeError {
                    Text(error)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.warning)
                }
            }
            .padding(DGSpacing.sm)
        }
    }

    private func membersSection(members: [TeamMember]) -> some View {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            HStack {
                Text("멤버 (\(members.count))")
                    .font(DGFont.headline)
                Spacer()
            }
            ForEach(members) { member in
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
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(DGColor.textSecondary)
            Spacer()
            Text(value)
        }
        .font(DGFont.callout)
    }

    private func displayDay(_ code: String) -> String {
        switch code {
        case "MON": "월"
        case "TUE": "화"
        case "WED": "수"
        case "THU": "목"
        case "FRI": "금"
        case "SAT": "토"
        case "SUN": "일"
        default: code
        }
    }
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

> 만약 `Team`의 `activityDays`, `activityTime` 등 필드명이 코드와 다르면 `Features/Team/Sources/Domain/Entities/Team.swift` 확인 후 맞춤.

---

### Task 4.3: HomeView에서 팀 카드 → TeamDetail Push

**Files:**
- Modify: `dugout-ios/Features/Home/Sources/Presentation/Views/HomeView.swift`

- [ ] **Step 1: NavigationStack에 path 추가, NavigationLink로 push**

`teamList(teams:)` 메서드의 `ForEach` 부분을 NavigationLink로 감싸기:

기존:
```swift
ForEach(teams) { team in
    DGCard {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            HStack {
                Text(team.teamName)
                    .font(DGFont.title3)
                Spacer()
                roleBadge(team.role)
            }
            Text("가입일: \(team.joinedAt.formatted(date: .abbreviated, time: .omitted))")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)
        }
    }
}
```

변경:
```swift
ForEach(teams) { team in
    NavigationLink {
        TeamDetailView(
            viewModel: TeamDetailViewModel(
                teamId: team.teamId,
                currentUserId: authViewModel.currentUser?.id
            )
        )
    } label: {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                HStack {
                    Text(team.teamName)
                        .font(DGFont.title3)
                        .foregroundStyle(DGColor.textPrimary)
                    Spacer()
                    roleBadge(team.role)
                }
                Text("가입일: \(team.joinedAt.formatted(date: .abbreviated, time: .omitted))")
                    .font(DGFont.footnote)
                    .foregroundStyle(DGColor.textSecondary)
            }
        }
    }
    .buttonStyle(.plain)
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Expected: BUILD SUCCEEDED

> 만약 `User`에 `id` 필드명이 다르면 (예: `userId`) Auth domain entity 확인 후 수정.

---

### Task 4.4: End-to-End 시뮬레이터 검증 + 최종 커밋

- [ ] **Step 1: 시뮬레이터 검증 시나리오**

dev-login 백엔드가 local profile에서만 동작하므로, `dugout-api` 가 로컬에서 실행 중인지 먼저 확인.

```bash
# 다른 터미널에서
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew bootRun --args='--spring.profiles.active=local'
# (백엔드가 :8080 포트에서 실행)
```

iOS 시뮬레이터에서:

1. 앱 실행 → 스플래시(1.2초) → 홈 탭(비로그인 안내 화면)
2. [팀 만들기] 탭 → LoginSheet → 닉네임 "주장A" 입력 → 로그인 → 자동으로 팀 만들기 폼 표시
3. 팀 정보 입력 (이름: "테스트팀", 지역: "서울 강남", 부수: 4부, 활동 요일: 토) → "생성"
4. 시트 dismiss → 홈에 "테스트팀" 카드 표시 (역할: 주장)
5. 팀 카드 탭 → TeamDetailView push → 팀 정보 + 초대 코드 영역 노출 + 멤버 1명(주장A) 표시
6. [초대 코드 생성] 탭 → 코드 표시 → 복사 버튼 동작 확인 (코드를 메모)
7. 마이페이지 탭 → 사용자 정보 표시 → [로그아웃] → 비로그인 복귀
8. [팀 가입하기] 탭 → LoginSheet → 닉네임 "용병B" → 로그인 → 자동으로 팀 가입 폼
9. 6번에서 메모한 초대 코드 입력 → "가입하기" → 시트 dismiss → 홈에 "테스트팀" (역할: 일반) 표시
10. 팀 카드 탭 → 멤버 2명(주장A, 용병B) 표시 + 초대 코드 영역 미표시 (MEMBER 시점)
11. 마이페이지 → 로그아웃 → 비로그인 복귀

- [ ] **Step 2: 모든 시나리오 통과 확인 후 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Team/Sources/Presentation/ dugout-ios/Features/Home/Sources/Presentation/Views/HomeView.swift
git commit -m "feat(ios): 팀 상세 화면 + 멤버 목록 + 초대 코드 (CAPTAIN 한정)

- TeamDetailViewModel: 팀 정보 + 멤버 병렬 fetch, myRole 판단
- TeamDetailView: 팀 정보, 초대 코드(CAPTAIN), 멤버 목록 표시
- HomeView 팀 카드 → NavigationLink로 push
- E2E: 생성 → 초대 코드 → 다른 계정 가입 → 양쪽 시점 검증 완료"
```

---

## 5. End-to-End 검증 시나리오 (M4 완료 시점)

위 Task 4.4의 시뮬레이터 검증 시나리오와 동일.

## 6. 커밋 전 체크리스트

- [ ] `xcodebuild ... build` 모두 성공
- [ ] Swift 6 Strict Concurrency 경고 없음
- [ ] 시뮬레이터에서 11단계 시나리오 통과
- [ ] 백엔드 API 변경 없음 (확인: `git status dugout-api/` 깨끗)
- [ ] spec과 plan의 의도 차이 없음 (비로그인 진입, deferred auth, 2탭, CAPTAIN 권한 분기)

## 7. 후속 작업 (이번 plan 범위 외)

- 팀 정보 수정 (`PUT /teams/{id}`)
- 멤버 추방·역할 변경 (`PUT/DELETE /members/{memberId}`)
- 팀 가입 미리보기 (백엔드 `GET /teams/by-invite-code/{code}` + UX 개선)
- OAuth 실제 통합 (카카오/네이버/구글/애플)
- 백엔드 `GET /api/v1/users/me` 추가 (사용자 정보 풍부화)
- 일정·출석 feature (홈에 위젯으로 통합)
- 카카오 알림톡, FCM, 푸시 알림
