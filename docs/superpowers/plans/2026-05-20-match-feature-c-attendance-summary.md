# Phase 3 MATCH-C Implementation Plan: 출석 요약 (주장 전용)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** iOS `DugoutMatchFeature` 모듈에 MATCH-5 출석 요약 화면(주장 전용)을 추가하고, MATCH-3 의 "전체 보기" 진입점을 토스트에서 NavigationLink push 로 교체한다. 푸시 알림 인프라는 본 PR 범위 외이며 알림 버튼은 placeholder toast 안내만 노출.

**Architecture:** `DugoutMatchFeature` 안에 `TeamMember` minimal Domain/Data 신설 (모듈 경계 유지). `MatchAttendanceSummaryViewModel` 이 `AttendanceRepository.fetchSummary` + `TeamMemberRepository.fetchMembers` 를 `async let` 으로 병렬 호출하고, 클라이언트가 미응답자(active 멤버 − 응답자) 를 계산. 필터 4종 + `AttendanceVoteRow` 공통 컴포넌트 추출. 백엔드 변경 없음.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, @Observable, Tuist 4.x, DugoutCoreNetwork (Alamofire wrapper). 백엔드 변경 없음 (Spring Boot 3.4.1).

---

## 0. 사전 준비 — 베이스 현황 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령

```bash
# iOS — 매 task 완료 시 실행
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build

# 새 파일 .xcodeproj 등록 (Phase 3-B M5 에서 한 번 필요했던 경우 대비)
tuist generate --no-open

# 백엔드 baseline (M6 에서만)
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

> 검증 단위 = `xcodebuild -quiet build` 성공 (warnings 0). iOS Unit Test 타겟 미존재. 새 파일을 만든 milestone(M1, M2, M3, M4) 끝마다 `tuist generate --no-open` 1회 실행 권장.

### 베이스 브랜치

```bash
cd /Users/heetae/Documents/Source/Dugout
git checkout main
git pull origin main
git checkout -b feature/phase3-match-c
```

### 백엔드 응답 JSON 참조

`GET /api/v1/teams/{teamId}/members` → `[TeamMemberResponse]`:

```json
[
  {
    "id": 1,
    "user_id": 10,
    "nickname": "박희태",
    "profile_img_url": null,
    "role": "CAPTAIN",
    "jersey_number": 7,
    "positions": ["P", "SS"],
    "is_active": true,
    "joined_at": "2026-03-01T10:00:00"
  }
]
```

`GET /api/v1/matches/{matchId}/attendance` 는 Phase 3-B M2 에서 이미 통합 — `AttendanceSummary` 도메인 변환 동작 검증 완료.

### 도메인 용어 (`CLAUDE.md`)

| 한글 | 영문 식별자 |
|---|---|
| 출석 | Attendance |
| 응답 / 투표 | Vote |
| 팀 | Team |
| 팀 멤버 | TeamMember |
| 주장 | Captain |
| 매니저 | Manager |
| 회계 | Accountant |

### 재사용 / 신규 / 수정 파일

**재사용 (변경 없음)**:
- iOS Domain (3-B): `AttendanceStatus`, `AttendanceVote`, `AttendanceSummary`, `AttendanceRepository(+Impl)`, `Match`, `MatchStatus`
- DesignSystem: `DGCard`, `DGSegmentedControl`, `DGToast(item:)`, `DGLoadingState`, `DGErrorState`, `DGEmptyState`, `DGColor/.danger/.p50/.p500/.p600/.c0/.c100/.c200/.c300/.c500/.c700`, `DGSpacing`, `DGFont`, `DGRadius`
- Network: `APIClient`, `APIEndpoint`, `APIError`
- 백엔드: 전부

**신규 (iOS)**:
- `dugout-ios/Features/Match/Sources/Domain/Entities/TeamRole.swift`
- `dugout-ios/Features/Match/Sources/Domain/Entities/TeamMemberRef.swift`
- `dugout-ios/Features/Match/Sources/Domain/Repositories/TeamMemberRepository.swift`
- `dugout-ios/Features/Match/Sources/Data/DTOs/TeamMemberRefDTO.swift`
- `dugout-ios/Features/Match/Sources/Data/Repositories/TeamMemberRepositoryImpl.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceVoteRow.swift`
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchAttendanceSummaryViewModel.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchAttendanceSummaryView.swift`

**수정 (iOS)**:
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift` (M3: voteRow 호출부 + shortTime/timeFormatter 제거, M5: summaryButton 교체 + dgToast 제거)
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift` (M5: `tapSummary()` + `toast` 프로퍼티 제거)

**수정 (문서)**:
- `docs/TDD.md` (M6: MATCH-5 항목 "후속" → "완료")

### DG 컴포넌트 시그니처 (재확인됨, plan 코드에 이미 반영)

- `DGSegmentedControl<T: Hashable & Sendable>(options: [T], selection: Binding<T>, title: (T) -> String)` — per-tab disable 미지원
- `DGToastItem(message: String, kind: .info | .success | .warning | .danger)` + `.dgToast(item: Binding<DGToastItem?>)`
- `TeamRole` 기존 정의 (Home/Team): `displayName` 헬퍼만, koreanLabel 아님

---

## Milestone 1 — TeamMember Domain 레이어 + 브랜치

### Task 1.1: feature/phase3-match-c 브랜치 생성

- [ ] **Step 1: main 깨끗한 상태에서 브랜치 생성**

```bash
cd /Users/heetae/Documents/Source/Dugout
git status   # working tree clean 확인
git checkout main
git pull origin main
git checkout -b feature/phase3-match-c
git branch --show-current   # feature/phase3-match-c 확인
```

### Task 1.2: TeamRole enum

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/TeamRole.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamRole.swift
//  DugoutMatchFeature
//
//  HomeFeature / TeamFeature 의 TeamRole 과 동일한 정의 —
//  Feature 독립 원칙에 따라 각자 소유 (TeamFeature TeamMember.swift 주석 참조).
//

import Foundation

public enum TeamRole: String, Sendable, Hashable, CaseIterable {
    case captain    = "CAPTAIN"
    case manager    = "MANAGER"
    case accountant = "ACCOUNTANT"
    case member     = "MEMBER"

    public var displayName: String {
        switch self {
        case .captain: "주장"
        case .manager: "매니저"
        case .accountant: "회계"
        case .member: "일반"
        }
    }
}
```

### Task 1.3: TeamMemberRef struct

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/TeamMemberRef.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamMemberRef.swift
//  DugoutMatchFeature
//
//  팀 멤버 식별·표시용 minimal 도메인 타입. MATCH-5 미응답자 노출에 사용.
//  백엔드 TeamMemberResponse 의 부분 집합 (positions/joinedAt 미포함 — 본 PR 범위 외).
//

import Foundation

public struct TeamMemberRef: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64            // TeamMember.id
    public let userId: Int64        // 응답자 매칭 키
    public let nickname: String
    public let profileImgUrl: String?
    public let jerseyNumber: Int?
    public let role: TeamRole
    public let isActive: Bool

    public init(
        id: Int64,
        userId: Int64,
        nickname: String,
        profileImgUrl: String?,
        jerseyNumber: Int?,
        role: TeamRole,
        isActive: Bool
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.profileImgUrl = profileImgUrl
        self.jerseyNumber = jerseyNumber
        self.role = role
        self.isActive = isActive
    }
}
```

### Task 1.4: TeamMemberRepository 프로토콜

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Repositories/TeamMemberRepository.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamMemberRepository.swift
//  DugoutMatchFeature
//

import Foundation

public protocol TeamMemberRepository: Sendable {
    /// 팀 멤버 전체 조회 (active + inactive 모두).
    /// 백엔드 GET /api/v1/teams/{teamId}/members 호출.
    func fetchMembers(teamId: Int64) async throws -> [TeamMemberRef]
}
```

### Task 1.5: M1 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공, warnings 0.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Domain/Entities/TeamRole.swift \
        dugout-ios/Features/Match/Sources/Domain/Entities/TeamMemberRef.swift \
        dugout-ios/Features/Match/Sources/Domain/Repositories/TeamMemberRepository.swift
git commit -m "feat(ios): TeamMember Domain 레이어 (Phase 3 MATCH-C 1/6)"
```

---

## Milestone 2 — TeamMember Data 레이어

### Task 2.1: TeamMemberRefDTO

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/DTOs/TeamMemberRefDTO.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamMemberRefDTO.swift
//  DugoutMatchFeature
//
//  백엔드 TeamMemberResponse 매핑 (부분 집합).
//  positions/joinedAt 필드는 Decodable 이 unknown key 로 자동 무시 — Jackson 기본 동작과 호환.
//

import Foundation

struct TeamMemberRefDTO: Decodable, Sendable {
    let id: Int64
    let userId: Int64
    let nickname: String
    let profileImgUrl: String?
    let role: String
    let jerseyNumber: Int?
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case id, nickname, role
        case userId         = "user_id"
        case profileImgUrl  = "profile_img_url"
        case jerseyNumber   = "jersey_number"
        case isActive       = "is_active"
    }

    func toDomain() -> TeamMemberRef? {
        guard let parsedRole = TeamRole(rawValue: role) else { return nil }
        return TeamMemberRef(
            id: id,
            userId: userId,
            nickname: nickname,
            profileImgUrl: profileImgUrl,
            jerseyNumber: jerseyNumber,
            role: parsedRole,
            isActive: isActive
        )
    }
}
```

### Task 2.2: TeamMemberRepositoryImpl

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/Repositories/TeamMemberRepositoryImpl.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  TeamMemberRepositoryImpl.swift
//  DugoutMatchFeature
//

import Foundation
import DugoutCoreNetwork

public struct TeamMemberRepositoryImpl: TeamMemberRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchMembers(teamId: Int64) async throws -> [TeamMemberRef] {
        let endpoint = APIEndpoint(path: "/api/v1/teams/\(teamId)/members")
        let dtos: [TeamMemberRefDTO] = try await client.request(endpoint)
        return dtos.compactMap { $0.toDomain() }
    }
}
```

### Task 2.3: M2 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Data/DTOs/TeamMemberRefDTO.swift \
        dugout-ios/Features/Match/Sources/Data/Repositories/TeamMemberRepositoryImpl.swift
git commit -m "feat(ios): TeamMember Data 레이어 (Phase 3 MATCH-C 2/6)"
```

---

## Milestone 3 — AttendanceVoteRow 공통 컴포넌트 추출

### Task 3.1: AttendanceVoteRow 신규 파일

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceVoteRow.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceVoteRow.swift
//  DugoutMatchFeature
//
//  MATCH-3 (경기 상세 / 출석 현황) + MATCH-5 (출석 요약) 공통 row.
//

import SwiftUI
import DugoutDesignSystem

struct AttendanceVoteRow: View {
    let vote: AttendanceVote

    var body: some View {
        HStack(spacing: DGSpacing.sm) {
            Text(vote.status.emoji)
            Text(vote.nickname).dgText(.bodyText)
            if let reason = vote.reason, !reason.isEmpty {
                Text("· \(reason)")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                    .lineLimit(1)
            }
            Spacer()
            Text(Self.shortTime(vote.respondedAt))
                .dgText(.label)
                .foregroundStyle(DGColor.c500)
        }
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "HH:mm"
        return f
    }()

    private static func shortTime(_ date: Date) -> String {
        timeFormatter.string(from: date)
    }
}
```

### Task 3.2: MatchDetailView 의 voteRow 호출부 + shortTime/timeFormatter 제거

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`

- [ ] **Step 1: 호출부 교체**

Find (around `MatchDetailView.attendanceSummaryCard`):

```swift
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(summary.votes) { vote in
                            voteRow(vote)
                        }
                    }
```

Replace with:

```swift
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(summary.votes) { vote in
                            AttendanceVoteRow(vote: vote)
                        }
                    }
```

- [ ] **Step 2: voteRow private 메서드 제거**

Find the entire block:

```swift
    private func voteRow(_ vote: AttendanceVote) -> some View {
        HStack(spacing: DGSpacing.sm) {
            Text(vote.status.emoji)
            Text(vote.nickname).dgText(.bodyText)
            if let reason = vote.reason, !reason.isEmpty {
                Text("· \(reason)")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                    .lineLimit(1)
            }
            Spacer()
            Text(Self.shortTime(vote.respondedAt))
                .dgText(.label)
                .foregroundStyle(DGColor.c500)
        }
    }
```

Delete it entirely.

- [ ] **Step 3: timeFormatter + shortTime 제거**

Find and delete:

```swift
    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "HH:mm"
        return f
    }()
```

Find and delete:

```swift
    private static func shortTime(_ date: Date) -> String {
        timeFormatter.string(from: date)
    }
```

> 다른 static formatter (matchDateFormatter, deadlineFormatter, respondedAtFormatter, sheetTitleFormatter 가 M5 에서 추가됨) 는 그대로 유지. `shortTime` 만 제거.

### Task 3.3: M3 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공. MatchDetailView 외 사용처 없으므로 `shortTime` 제거가 다른 곳에 영향 없음.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceVoteRow.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift
git commit -m "refactor(ios): AttendanceVoteRow 공통 컴포넌트 추출 (Phase 3 MATCH-C 3/6)"
```

---

## Milestone 4 — MatchAttendanceSummaryViewModel + View

### Task 4.1: MatchAttendanceSummaryViewModel

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchAttendanceSummaryViewModel.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  MatchAttendanceSummaryViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork
import DugoutDesignSystem

@MainActor
@Observable
public final class MatchAttendanceSummaryViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded(Snapshot)
        case failed(String)
    }

    public struct Snapshot: Sendable, Equatable {
        public let summary: AttendanceSummary
        public let activeMembers: [TeamMemberRef]
    }

    public enum Filter: Sendable, Equatable, Hashable, CaseIterable {
        case all, attend, absent, pending

        public var displayName: String {
            switch self {
            case .all:     "전체"
            case .attend:  "참가"
            case .absent:  "불참"
            case .pending: "미응답"
            }
        }
    }

    public private(set) var state: State = .idle
    public var filter: Filter = .all
    public var toast: DGToastItem? = nil

    public let matchId: Int64
    public let teamId: Int64
    private let attendanceRepository: any AttendanceRepository
    private let teamMemberRepository: any TeamMemberRepository

    public init(
        matchId: Int64,
        teamId: Int64,
        attendanceRepository: any AttendanceRepository = AttendanceRepositoryImpl(),
        teamMemberRepository: any TeamMemberRepository = TeamMemberRepositoryImpl()
    ) {
        self.matchId = matchId
        self.teamId = teamId
        self.attendanceRepository = attendanceRepository
        self.teamMemberRepository = teamMemberRepository
    }

    public func load() async {
        state = .loading
        async let summaryFetch = attendanceRepository.fetchSummary(matchId: matchId)
        async let membersFetch = teamMemberRepository.fetchMembers(teamId: teamId)
        do {
            let (summary, members) = try await (summaryFetch, membersFetch)
            let active = members.filter { $0.isActive }
            state = .loaded(Snapshot(summary: summary, activeMembers: active))
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("출석 현황을 불러오지 못했습니다")
        }
    }

    public func tapNotify(_ member: TeamMemberRef) {
        toast = DGToastItem(
            message: "알림 기능은 준비 중이에요",
            kind: .info
        )
    }

    // MARK: - Derived

    public var loadedSnapshot: Snapshot? {
        if case .loaded(let s) = state { return s }
        return nil
    }

    /// 응답자 row (현재 filter 기준). 미정은 .all 에서 별도 섹션으로 분리되므로 제외.
    public var voteRows: [AttendanceVote] {
        guard let s = loadedSnapshot else { return [] }
        let all = s.summary.votes
        switch filter {
        case .all:     return all.filter { $0.status != .maybe }
        case .attend:  return all.filter { [.attend, .late, .earlyLeave].contains($0.status) }
        case .absent:  return all.filter { $0.status == .absent }
        case .pending: return []
        }
    }

    /// 미정 응답자 (filter == .all 에서만).
    public var maybeRows: [AttendanceVote] {
        guard filter == .all, let s = loadedSnapshot else { return [] }
        return s.summary.votes.filter { $0.status == .maybe }
    }

    /// 미응답자 row.
    public var pendingRows: [TeamMemberRef] {
        guard let s = loadedSnapshot else { return [] }
        switch filter {
        case .all, .pending:
            let responded = Set(s.summary.votes.map { $0.userId })
            return s.activeMembers
                .filter { !responded.contains($0.userId) }
                .sorted { $0.nickname < $1.nickname }
        case .attend, .absent:
            return []
        }
    }

    /// DGSegmentedControl 옵션 — 미응답자 0명이면 .pending 제거.
    public var availableFilters: [Filter] {
        guard let s = loadedSnapshot else { return Filter.allCases }
        let responded = Set(s.summary.votes.map { $0.userId })
        let pendingCount = s.activeMembers.filter { !responded.contains($0.userId) }.count
        if pendingCount == 0 {
            return [.all, .attend, .absent]
        }
        return Filter.allCases
    }
}
```

### Task 4.2: MatchAttendanceSummaryView

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchAttendanceSummaryView.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  MatchAttendanceSummaryView.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct MatchAttendanceSummaryView: View {
    @State private var viewModel: MatchAttendanceSummaryViewModel

    public init(matchId: Int64, teamId: Int64) {
        _viewModel = State(
            initialValue: MatchAttendanceSummaryViewModel(matchId: matchId, teamId: teamId)
        )
    }

    public var body: some View {
        content
            .background(DGColor.c100)
            .navigationTitle("출석 현황")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.load() }
            .dgToast(item: $viewModel.toast)
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .idle, .loading:
            DGLoadingState(preset: .list)
                .frame(maxHeight: .infinity)
        case .failed(let message):
            DGErrorState(message: message) {
                Task { await viewModel.load() }
            }
        case .loaded(let snapshot):
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    statsCard(snapshot.summary)
                    filterSegment
                    voteSection
                    maybeSection
                    pendingSection
                }
                .padding(.horizontal, DGSpacing.lg)
                .padding(.vertical, DGSpacing.lg)
            }
        }
    }

    // MARK: - 1) 통계 카드 (MATCH-3 와 동일 시각)

    private func statsCard(_ summary: AttendanceSummary) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                HStack(spacing: DGSpacing.md) {
                    statColumn(label: "참가", count: summary.count(of: .attend), tone: .primary)
                    statColumn(label: "불참", count: summary.count(of: .absent), tone: .danger)
                    statColumn(label: "미응답", count: summary.pendingCount, tone: .neutral)
                }
                HStack(spacing: DGSpacing.md) {
                    Text("미정 \(summary.count(of: .maybe))").dgText(.subText)
                    Text("·").foregroundStyle(DGColor.c500)
                    Text("늦참 \(summary.count(of: .late))").dgText(.subText)
                    Text("·").foregroundStyle(DGColor.c500)
                    Text("조퇴 \(summary.count(of: .earlyLeave))").dgText(.subText)
                }
                .foregroundStyle(DGColor.c500)
            }
        }
    }

    private enum StatTone { case primary, danger, neutral }

    private func statColumn(label: String, count: Int, tone: StatTone) -> some View {
        VStack(spacing: DGSpacing.xs) {
            Text("\(count)")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(toneColor(tone, isDim: count == 0))
            Text(label)
                .dgText(.label)
                .foregroundStyle(DGColor.c500)
        }
        .frame(maxWidth: .infinity)
    }

    private func toneColor(_ tone: StatTone, isDim: Bool) -> Color {
        if isDim { return DGColor.c300 }
        switch tone {
        case .primary: return DGColor.p500
        case .danger: return DGColor.danger
        case .neutral: return DGColor.c700
        }
    }

    // MARK: - 2) 필터 segment

    private var filterSegment: some View {
        DGSegmentedControl(
            options: viewModel.availableFilters,
            selection: $viewModel.filter
        ) { $0.displayName }
    }

    // MARK: - 3) 응답자 섹션

    @ViewBuilder
    private var voteSection: some View {
        let rows = viewModel.voteRows
        if !rows.isEmpty {
            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.md) {
                    Text("응답자 \(rows.count)명")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                    Divider()
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(rows) { vote in
                            AttendanceVoteRow(vote: vote)
                        }
                    }
                }
            }
        }
    }

    // MARK: - 4) 미정 섹션 (.all 일 때만)

    @ViewBuilder
    private var maybeSection: some View {
        let rows = viewModel.maybeRows
        if !rows.isEmpty {
            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.md) {
                    Text("미정 \(rows.count)명")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                    Divider()
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(rows) { vote in
                            AttendanceVoteRow(vote: vote)
                        }
                    }
                }
            }
        }
    }

    // MARK: - 5) 미응답자 섹션

    @ViewBuilder
    private var pendingSection: some View {
        let rows = viewModel.pendingRows
        if !rows.isEmpty {
            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.md) {
                    Text("미응답 \(rows.count)명")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                    Divider()
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(rows) { member in
                            pendingRow(member)
                        }
                    }
                }
            }
        }
    }

    private func pendingRow(_ member: TeamMemberRef) -> some View {
        HStack(spacing: DGSpacing.sm) {
            Text("👤")
            VStack(alignment: .leading, spacing: 2) {
                Text(member.nickname).dgText(.bodyText)
                let meta = memberMetaLabel(member)
                if !meta.isEmpty {
                    Text(meta)
                        .dgText(.label)
                        .foregroundStyle(DGColor.c500)
                }
            }
            Spacer()
            notifyButton(member)
        }
    }

    private func memberMetaLabel(_ member: TeamMemberRef) -> String {
        var parts: [String] = []
        if let jersey = member.jerseyNumber {
            parts.append("#\(jersey)")
        }
        if member.role != .member {
            parts.append(member.role.displayName)
        }
        return parts.joined(separator: " · ")
    }

    private func notifyButton(_ member: TeamMemberRef) -> some View {
        Button {
            viewModel.tapNotify(member)
        } label: {
            Text("알림")
                .font(DGFont.label)
                .foregroundStyle(DGColor.p600)
                .padding(.horizontal, DGSpacing.md)
                .padding(.vertical, DGSpacing.xs)
                .background(DGColor.p50)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
```

### Task 4.3: M4 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

> View 가 아직 어디서도 호출되지 않아 dead-code 경고는 SwiftUI View 특성상 없음.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchAttendanceSummaryViewModel.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/MatchAttendanceSummaryView.swift
git commit -m "feat(ios): MATCH-5 출석 요약 화면 (Phase 3 MATCH-C 4/6)"
```

---

## Milestone 5 — MATCH-3 진입점 교체

### Task 5.1: MatchDetailViewModel 에서 tapSummary + toast 제거

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift`

- [ ] **Step 1: toast 프로퍼티 제거**

Find:

```swift
    public private(set) var state: State = .idle
    public var presentVoteSheet: Bool = false
    public var toast: DGToastItem? = nil
```

Replace with:

```swift
    public private(set) var state: State = .idle
    public var presentVoteSheet: Bool = false
```

- [ ] **Step 2: tapSummary 메서드 제거**

Find:

```swift
    public func tapSummary() {
        toast = DGToastItem(
            message: "출석 요약은 다음 업데이트에 제공돼요",
            kind: .info
        )
    }
```

Delete the entire method.

- [ ] **Step 3: DugoutDesignSystem import 점검**

`MatchDetailViewModel.swift` 의 import 들 확인 — `import DugoutDesignSystem` 이 `DGToastItem` 때문에 들어가 있는데, toast 가 사라지므로 더 이상 필요 없을 수 있음. ViewModel 내부에서 다른 DG 타입을 사용하지 않는지 확인:

```bash
grep -n "DGToastItem\|DGColor\|DGFont\|DGSpacing\|DG[A-Z]" \
  dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift
```

사용처가 없으면 `import DugoutDesignSystem` 제거. 사용처가 있으면 유지.

### Task 5.2: MatchDetailView 의 summaryButton 교체 + dgToast 제거

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`

- [ ] **Step 1: dgToast modifier 제거**

Find in `MatchDetailView.body`:

```swift
            .task { await viewModel.load() }
            .dgToast(item: $viewModel.toast)
            .sheet(isPresented: $viewModel.presentVoteSheet) {
```

Replace with (toast 라인만 제거):

```swift
            .task { await viewModel.load() }
            .sheet(isPresented: $viewModel.presentVoteSheet) {
```

- [ ] **Step 2: summaryButton 교체**

Find:

```swift
    private var summaryButton: some View {
        DGButton("전체 보기", style: .secondary) {
            viewModel.tapSummary()
        }
    }
```

Replace with:

```swift
    @ViewBuilder
    private var summaryButton: some View {
        if let detail = viewModel.loadedDetail {
            NavigationLink {
                MatchAttendanceSummaryView(
                    matchId: viewModel.matchId,
                    teamId: detail.match.teamId
                )
            } label: {
                Text("전체 보기")
                    .font(DGFont.pretendard(.semibold, size: 15))
                    .foregroundStyle(DGColor.p500)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(DGColor.c0)
                    .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
                    .overlay(
                        RoundedRectangle(cornerRadius: DGRadius.button)
                            .stroke(DGColor.p500, lineWidth: 1)
                    )
            }
            .buttonStyle(.plain)
        }
    }
```

### Task 5.3: M5 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

> `MatchAttendanceSummaryView` 호출이 새로 추가됐으므로 만약 .xcodeproj 미등록 에러 발생 시 `tuist generate --no-open` 1회 실행 후 재빌드.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift
git commit -m "feat(ios): MATCH-3 전체 보기 진입점 MATCH-5 push 로 교체 (Phase 3 MATCH-C 5/6)"
```

---

## Milestone 6 — 통합 (검증 + 문서 + 머지)

> 시뮬레이터 수동 시나리오와 main 머지는 controller 가 처리. implementer 는 Task 6.1~6.4 까지.

### Task 6.1: iOS 빌드 + 백엔드 baseline

- [ ] **Step 1: iOS 빌드**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공, warnings 0.

- [ ] **Step 2: 백엔드 baseline 점검**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

Expected: 에러 0. 백엔드 변경 없으므로 baseline 그대로.

### Task 6.2: PII 로그 가드 검증

- [ ] **Step 1: nickname / reason / member 로깅 검사**

```bash
cd /Users/heetae/Documents/Source/Dugout
grep -rEn "(print|os_log|NSLog).*(nickname|reason|member)" \
  dugout-ios/Features/Match/Sources/
```

Expected: 0 lines. 발견되면 제거 후 재실행.

### Task 6.3: docs/TDD.md 갱신

**Files:**
- Modify: `docs/TDD.md`

- [ ] **Step 1: DugoutMatchFeature 단락 갱신**

`docs/TDD.md` 의 DugoutMatchFeature 설명 단락 (Phase 3-B 에서 이미 갱신된 곳) 을 찾아 MATCH-5 도 포함하도록 수정:

```bash
grep -n "DugoutMatchFeature\|MATCH-1.*MATCH-2.*MATCH-3.*MATCH-4" docs/TDD.md | head -5
```

기존 문장:

> `DugoutMatchFeature`는 Phase 3 MATCH-A·B에서 추가된 모듈로, 백엔드 `/api/v1/teams/{teamId}/matches`, `/api/v1/matches/{matchId}`, `/api/v1/matches/{matchId}/attendance` 엔드포인트를 사용한다. 경기 일정(MATCH-1)·등록(MATCH-2)·상세(MATCH-3)·출석 응답(MATCH-4)을 제공한다. 출석 요약(MATCH-5)·카카오 공유는 Phase 3-C 예정이다.

다음으로 교체:

> `DugoutMatchFeature`는 Phase 3 MATCH-A·B·C에서 추가된 모듈로, 백엔드 `/api/v1/teams/{teamId}/matches`, `/api/v1/matches/{matchId}`, `/api/v1/matches/{matchId}/attendance`, `/api/v1/teams/{teamId}/members` 엔드포인트를 사용한다. 경기 일정(MATCH-1)·등록(MATCH-2)·상세(MATCH-3)·출석 응답(MATCH-4)·출석 요약(MATCH-5, 주장 전용)을 제공한다. 푸시 알림·카카오 공유는 후속 Phase (3-C.1 / 3-C.2 / 3-C.3) 예정이다.

또 의존성 다이어그램 코멘트 `# Phase 3 MATCH-A·B (일정·등록·상세·출석 응답)` 을 다음으로 교체:

```
DugoutMatchFeature          # Phase 3 MATCH-A·B·C (일정·등록·상세·출석 응답·출석 요약)
```

### Task 6.4: 통합 커밋

```bash
cd /Users/heetae/Documents/Source/Dugout
git status
git diff --stat
git add docs/TDD.md
git commit -m "docs(tdd): Phase 3 MATCH-C 반영 (출석 요약·MATCH-5)

DugoutMatchFeature 책임 단락에 MATCH-5 추가, 의존성 다이어그램 코멘트
'MATCH-A·B' → 'MATCH-A·B·C' 갱신. 푸시 알림/카카오 공유는 후속 Phase
명시."
```

> Task 6.5 (시뮬레이터 수동 시나리오) 와 Task 6.6 (main 머지) 는 controller 가 직접 수행. implementer 는 여기서 종료.

### Task 6.5: 시뮬레이터 수동 시나리오 검증 (CONTROLLER 영역)

- [ ] **Step 1: 백엔드 로컬 기동**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew bootRun --args='--spring.profiles.active=local'
```

별도 터미널 health check:

```bash
curl -s -w '\nHTTP %{http_code}\n' http://localhost:8080/api/v1/health
```

Expected: HTTP 200.

- [ ] **Step 2: Xcode 시뮬레이터 실행 후 다음 시나리오 통과 확인**

1. 주장 로그인 → 일정 탭 → 경기 카드 탭 → MatchDetailView 진입
2. 하단 "전체 보기" 버튼 탭 → `MatchAttendanceSummaryView` push (slide-in)
3. 통계 카드 수치가 MATCH-3 의 출석 현황 카드 수치와 정확히 일치
4. 필터 "전체" → 응답자(미정 제외) + 미정 별도 + 미응답자 모두 노출
5. 필터 "참가" → attend/late/earlyLeave 만, 미정·불참·미응답 미노출
6. 필터 "불참" → absent 만
7. 필터 "미응답" → 미응답자만, 각 행에 "알림" 버튼
8. "알림" 버튼 탭 → "알림 기능은 준비 중이에요" 토스트 3초 후 자동 사라짐
9. 미응답자 0명 케이스 — segment 에 .pending 탭 미노출 (가능하면 백엔드에 모든 멤버 응답 데이터를 만들어 검증, 어려우면 코드 리뷰로 대체)

각 결과를 메모.

### Task 6.6: main 머지 + 브랜치 정리 (CONTROLLER 영역)

```bash
cd /Users/heetae/Documents/Source/Dugout
git checkout main
git merge --no-ff feature/phase3-match-c -m "Merge branch 'feature/phase3-match-c'

Phase 3 MATCH-C 완료:
- MATCH-5 출석 요약 화면 (주장 전용, 필터 4종, 미응답자 식별)
- AttendanceVoteRow 공통 컴포넌트 추출 (MATCH-3 + MATCH-5)
- TeamMember minimal Domain/Data 신설 (Match 모듈 자체)
- MATCH-3 '전체 보기' 토스트 → NavigationLink push 로 교체
- docs/TDD.md DugoutMatchFeature 책임 갱신"

git branch -d feature/phase3-match-c
git push origin main
```

---

## 검증 체크리스트 (Phase 3 MATCH-C 완료 조건)

- [ ] `xcodebuild -quiet build` 성공 (warnings 0)
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공
- [ ] 수동 시나리오 1~8 통과
- [ ] 시나리오 9 코드 리뷰 — `availableFilters` 가 미응답자 0명일 때 `.pending` 제외
- [ ] Swift 6 Sendable 위반 0
- [ ] PII 로그 노출 0 (grep 0건)
- [ ] 도메인 용어 준수 (Attendance, Match, Team, TeamMember, Vote)
- [ ] 새 ErrorCode 추가 없음 (백엔드 변경 0)
- [ ] `docs/TDD.md` MATCH-5 반영 커밋 포함
- [ ] feature/phase3-match-c 의 6 commit + main 머지 + 브랜치 삭제 완료
- [ ] origin/main push 완료

---

## 추후 Phase (스코프 외)

- **Phase 3-C.1**: APNs/FCM 인프라 — 외부 키 발급 + Firebase 프로젝트 + GoogleService-Info.plist + 백엔드 Firebase Admin SDK
- **Phase 3-C.2**: 디바이스 토큰 등록 endpoint + `POST /matches/{id}/notify` + 24h 쿨다운 + iOS 알림 트리거
- **Phase 3-C.3** (선택): 카카오 공유 — KakaoSDK, URL Scheme, 요약 텍스트 generate
- **MATCH-5 polish**: pull-to-refresh, 멤버 포지션 표시, stagger 40ms fade 애니메이션, `DGSmallButton`/`DGPill` 컴포넌트 추출
- **TeamRole 통합 리팩터링**: Home/Team/Match 3중 중복을 별도 공유 모듈로 추출
