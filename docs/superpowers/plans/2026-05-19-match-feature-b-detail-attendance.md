# Phase 3 MATCH-B Implementation Plan: 경기 상세 + 출석 응답

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** iOS `DugoutMatchFeature` 모듈에 MATCH-3(경기 상세) + MATCH-4(출석 응답) 두 화면을 추가하고, `MatchListView` 셀 탭으로 상세 진입이 가능하도록 와이어업한다. 백엔드 Attendance 도메인은 이미 구현 완료 상태이므로 iOS 단독 작업.

**Architecture:** `DugoutMatchFeature` 내부에 `Sources/Attendance/` 서브 폴더 신설(같은 Tuist 타겟, 폴더로 격리). Clean Architecture(Data/Domain/Presentation) 패턴 유지. NavigationStack 은 `App/Sources/ScheduleTabHost.swift` 에서 도입하여 다른 탭에 영향 없음. 신규 응답 vs 응답 변경 분기는 클라이언트가 `existingVote == nil` 로 판단하고, `ALREADY_VOTED (409)` race 발생 시 자동 PUT 재시도.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, @Observable, Tuist 4.x, Alamofire (DugoutCoreNetwork 경유). 백엔드 변경 없음 (Spring Boot 3.4.1 + Kotlin).

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

# Tuist 재생성이 필요한 경우만 (Project.swift 변경 없음 → 거의 불필요)
tuist generate --no-open

# 백엔드 점검 (M6 에서만)
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

> **검증 단위 = `xcodebuild -quiet build` 성공.** iOS Unit Test 타겟은 Phase 2 이후이므로 TDD 적용 불가. 각 task 는 코드 작성 → 빌드 통과 → 커밋 순서.

### 베이스 브랜치

```bash
cd /Users/heetae/Documents/Source/Dugout
git checkout main
git pull origin main
git checkout -b feature/phase3-match-b
```

### 백엔드 응답 JSON 예시 (검증용 참고)

`GET /api/v1/matches/1/attendance` →

```json
{
  "match_id": 1,
  "total_members": 15,
  "responded_count": 2,
  "pending_count": 13,
  "status_counts": {
    "ATTEND": 1, "ABSENT": 0, "MAYBE": 1, "LATE": 0, "EARLY_LEAVE": 0
  },
  "votes": [
    {
      "id": 1, "match_id": 1, "user_id": 10, "nickname": "박희태",
      "status": "ATTEND", "reason": null, "responded_at": "2026-05-19T14:30:00"
    }
  ]
}
```

`POST /api/v1/matches/1/attendance` body → `{ "status": "ATTEND", "reason": null }` → `201 AttendanceResponse`

`PUT /api/v1/matches/1/attendance` body → 동일 → `200 AttendanceResponse`

### 백엔드 ErrorCode (재사용)

`NOT_TEAM_MEMBER`(403), `MATCH_NOT_FOUND`(404), `MATCH_ALREADY_CANCELLED`(400), `ALREADY_VOTED`(409), `VOTE_NOT_FOUND`(404), `VOTE_DEADLINE_PASSED`(400), `INVALID_ATTENDANCE_STATUS`(400). 한국어 메시지는 백엔드에서 그대로 노출.

### 재사용 / 신규 / 수정 파일

**재사용 (변경 없음)**:
- iOS Match 기존: `Match.swift`, `MatchStatus.swift` (`.cancelled` 케이스 존재), `MatchDTO.swift`, `MatchCalendarGrid.swift`, `CreateMatchView.swift`, `MatchListViewModel.swift`, `CreateMatchViewModel.swift`, `MatchRequestDTO.swift`
- DesignSystem 컴포넌트: `DGCard`, `DGButton`(string label only), `DGBadge`(variant `.dDay`, `.neutral`, `.captain`, `.manager` 등), `DGTextField`, `DGEmptyState`, `DGLoadingState`, `DGErrorState`, `DGToast`
- Network: `APIClient`, `APIEndpoint`, `APIEndpoint.json`, `APIError`(`.server(let response, _)` 에서 `response.code` 접근 가능)
- 백엔드: 전부 변경 없음

**신규 (iOS)**:
- `dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceStatus.swift`
- `dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceVote.swift`
- `dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceSummary.swift`
- `dugout-ios/Features/Match/Sources/Domain/Repositories/AttendanceRepository.swift`
- `dugout-ios/Features/Match/Sources/Data/DTOs/AttendanceDTO.swift`
- `dugout-ios/Features/Match/Sources/Data/DTOs/AttendanceRequestDTO.swift`
- `dugout-ios/Features/Match/Sources/Data/Repositories/AttendanceRepositoryImpl.swift`
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift`
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/AttendanceVoteViewModel.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceVoteSheet.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceStatusButton.swift`

**수정 (iOS)**:
- `dugout-ios/Features/Match/Sources/Domain/Repositories/MatchRepository.swift` (fetchDetail 메서드 추가)
- `dugout-ios/Features/Match/Sources/Data/Repositories/MatchRepositoryImpl.swift` (fetchDetail 구현 추가)
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchListView.swift` (`currentUserId` 파라미터 추가, NavigationLink + navigationDestination)
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchListViewModel.swift` (`currentUserId` 프로퍼티 추가)
- `dugout-ios/App/Sources/MainTabView.swift` (`ScheduleTabHost(authViewModel:)` 주입)
- `dugout-ios/App/Sources/ScheduleTabHost.swift` (`authViewModel` 의존성, `NavigationStack`, `currentUserId` 전파)

**수정 (문서)**:
- `docs/TDD.md` (Attendance 행 추가)

### 도메인 용어

| 한글 | 영문 식별자 |
|---|---|
| 출석 | Attendance |
| 응답 / 투표 | Vote |
| 응답자 | (voter) — 백엔드 표기 없음, 컨텍스트 명시는 `vote` 사용 |
| 참가 / 불참 / 미정 / 늦참 / 조퇴 | ATTEND / ABSENT / MAYBE / LATE / EARLY_LEAVE |

---

## Milestone 1 — Attendance Domain 레이어

### Task 1.1: 브랜치 생성

- [ ] **Step 1: feature/phase3-match-b 브랜치 생성**

```bash
cd /Users/heetae/Documents/Source/Dugout
git checkout main
git status   # clean 확인
git checkout -b feature/phase3-match-b
```

Expected: `Switched to a new branch 'feature/phase3-match-b'`.

### Task 1.2: AttendanceStatus enum

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceStatus.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceStatus.swift
//  DugoutMatchFeature
//
//  백엔드 AttendanceStatus enum (ATTEND/ABSENT/MAYBE/LATE/EARLY_LEAVE) 대응.
//

import Foundation

public enum AttendanceStatus: String, Sendable, Codable, CaseIterable, Hashable {
    case attend     = "ATTEND"
    case absent     = "ABSENT"
    case maybe      = "MAYBE"
    case late       = "LATE"
    case earlyLeave = "EARLY_LEAVE"

    public var koreanLabel: String {
        switch self {
        case .attend: "참가"
        case .absent: "불참"
        case .maybe: "미정"
        case .late: "늦참"
        case .earlyLeave: "조퇴"
        }
    }

    public var emoji: String {
        switch self {
        case .attend: "✅"
        case .absent: "❌"
        case .maybe: "❓"
        case .late: "⏰"
        case .earlyLeave: "🚪"
        }
    }
}
```

### Task 1.3: AttendanceVote struct

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceVote.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceVote.swift
//  DugoutMatchFeature
//
//  팀원 1인의 출석 응답 1건.
//

import Foundation

public struct AttendanceVote: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let matchId: Int64
    public let userId: Int64
    public let nickname: String
    public let status: AttendanceStatus
    public let reason: String?
    public let respondedAt: Date

    public init(
        id: Int64,
        matchId: Int64,
        userId: Int64,
        nickname: String,
        status: AttendanceStatus,
        reason: String?,
        respondedAt: Date
    ) {
        self.id = id
        self.matchId = matchId
        self.userId = userId
        self.nickname = nickname
        self.status = status
        self.reason = reason
        self.respondedAt = respondedAt
    }
}
```

### Task 1.4: AttendanceSummary struct

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceSummary.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceSummary.swift
//  DugoutMatchFeature
//
//  GET /api/v1/matches/{matchId}/attendance 응답에 대응.
//  팀 전체 인원 수, 응답 카운트, 상태별 카운트, 응답자 리스트를 묶음.
//

import Foundation

public struct AttendanceSummary: Sendable, Equatable {
    public let matchId: Int64
    public let totalMembers: Int
    public let respondedCount: Int
    public let pendingCount: Int
    public let statusCounts: [AttendanceStatus: Int]
    public let votes: [AttendanceVote]

    public init(
        matchId: Int64,
        totalMembers: Int,
        respondedCount: Int,
        pendingCount: Int,
        statusCounts: [AttendanceStatus: Int],
        votes: [AttendanceVote]
    ) {
        self.matchId = matchId
        self.totalMembers = totalMembers
        self.respondedCount = respondedCount
        self.pendingCount = pendingCount
        self.statusCounts = statusCounts
        self.votes = votes
    }

    /// 현재 로그인 사용자의 응답 추출 (없으면 nil).
    public func myVote(userId: Int64) -> AttendanceVote? {
        votes.first { $0.userId == userId }
    }

    /// 상태별 카운트 (없으면 0).
    public func count(of status: AttendanceStatus) -> Int {
        statusCounts[status] ?? 0
    }
}
```

### Task 1.5: AttendanceRepository 프로토콜 + Request 타입

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Repositories/AttendanceRepository.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceRepository.swift
//  DugoutMatchFeature
//

import Foundation

public protocol AttendanceRepository: Sendable {
    /// 경기의 출석 요약 + 응답자 리스트 조회.
    func fetchSummary(matchId: Int64) async throws -> AttendanceSummary

    /// 최초 응답 등록 (POST). 이미 응답이 있으면 ALREADY_VOTED(409) 발생.
    func createVote(matchId: Int64, request: AttendanceVoteRequest) async throws -> AttendanceVote

    /// 응답 변경 (PUT). 응답이 없으면 VOTE_NOT_FOUND(404) 발생.
    func updateVote(matchId: Int64, request: AttendanceVoteRequest) async throws -> AttendanceVote
}

public struct AttendanceVoteRequest: Sendable, Equatable {
    public let status: AttendanceStatus
    public let reason: String?

    public init(status: AttendanceStatus, reason: String? = nil) {
        self.status = status
        self.reason = reason
    }
}
```

### Task 1.6: M1 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공, warnings 0. (신규 파일들은 Tuist 의 `sourcesPath: "Features/Match/Sources"` 글롭에 자동 포함되므로 `tuist generate` 불필요.)

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceStatus.swift \
        dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceVote.swift \
        dugout-ios/Features/Match/Sources/Domain/Entities/AttendanceSummary.swift \
        dugout-ios/Features/Match/Sources/Domain/Repositories/AttendanceRepository.swift
git commit -m "feat(ios): Attendance Domain 레이어 (Phase 3 MATCH-B 1/6)"
```

---

## Milestone 2 — Attendance Data 레이어 + MatchRepository.fetchDetail

### Task 2.1: AttendanceDTO + AttendanceSummaryDTO

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/DTOs/AttendanceDTO.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceDTO.swift
//  DugoutMatchFeature
//
//  백엔드 AttendanceResponse / AttendanceSummaryResponse 매핑.
//  Spring Boot Jackson SNAKE_CASE 글로벌 설정이므로 CodingKeys 명시.
//

import Foundation

struct AttendanceVoteDTO: Decodable, Sendable {
    let id: Int64
    let matchId: Int64
    let userId: Int64
    let nickname: String
    let status: String
    let reason: String?
    let respondedAt: Date

    enum CodingKeys: String, CodingKey {
        case id, nickname, status, reason
        case matchId      = "match_id"
        case userId       = "user_id"
        case respondedAt  = "responded_at"
    }

    func toDomain() -> AttendanceVote? {
        guard let parsedStatus = AttendanceStatus(rawValue: status) else { return nil }
        return AttendanceVote(
            id: id,
            matchId: matchId,
            userId: userId,
            nickname: nickname,
            status: parsedStatus,
            reason: reason,
            respondedAt: respondedAt
        )
    }
}

struct AttendanceSummaryDTO: Decodable, Sendable {
    let matchId: Int64
    let totalMembers: Int
    let respondedCount: Int
    let pendingCount: Int
    let statusCounts: [String: Int]
    let votes: [AttendanceVoteDTO]

    enum CodingKeys: String, CodingKey {
        case matchId         = "match_id"
        case totalMembers    = "total_members"
        case respondedCount  = "responded_count"
        case pendingCount    = "pending_count"
        case statusCounts    = "status_counts"
        case votes
    }

    func toDomain() -> AttendanceSummary {
        // 백엔드는 모든 AttendanceStatus enum entry 를 statusCounts 에 포함하므로
        // 누락된 key 는 발생하지 않지만 방어적으로 0 처리.
        var counts: [AttendanceStatus: Int] = [:]
        for status in AttendanceStatus.allCases {
            counts[status] = statusCounts[status.rawValue] ?? 0
        }
        return AttendanceSummary(
            matchId: matchId,
            totalMembers: totalMembers,
            respondedCount: respondedCount,
            pendingCount: pendingCount,
            statusCounts: counts,
            votes: votes.compactMap { $0.toDomain() }
        )
    }
}
```

### Task 2.2: AttendanceRequestDTO

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/DTOs/AttendanceRequestDTO.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceRequestDTO.swift
//  DugoutMatchFeature
//

import Foundation

struct AttendanceVoteRequestDTO: Encodable, Sendable {
    let status: String      // AttendanceStatus rawValue ("ATTEND" 등)
    let reason: String?

    init(_ request: AttendanceVoteRequest) {
        self.status = request.status.rawValue
        self.reason = request.reason
    }
}
```

### Task 2.3: AttendanceRepositoryImpl

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/Repositories/AttendanceRepositoryImpl.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceRepositoryImpl.swift
//  DugoutMatchFeature
//

import Foundation
import DugoutCoreNetwork

public struct AttendanceRepositoryImpl: AttendanceRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchSummary(matchId: Int64) async throws -> AttendanceSummary {
        let endpoint = APIEndpoint(path: "/api/v1/matches/\(matchId)/attendance")
        let dto: AttendanceSummaryDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func createVote(
        matchId: Int64,
        request: AttendanceVoteRequest
    ) async throws -> AttendanceVote {
        let body = AttendanceVoteRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/attendance",
            method: .post,
            body: body
        )
        let dto: AttendanceVoteDTO = try await client.request(endpoint)
        guard let vote = dto.toDomain() else {
            throw APIError.decoding("AttendanceVoteDTO → AttendanceVote 변환 실패")
        }
        return vote
    }

    public func updateVote(
        matchId: Int64,
        request: AttendanceVoteRequest
    ) async throws -> AttendanceVote {
        let body = AttendanceVoteRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/attendance",
            method: .put,
            body: body
        )
        let dto: AttendanceVoteDTO = try await client.request(endpoint)
        guard let vote = dto.toDomain() else {
            throw APIError.decoding("AttendanceVoteDTO → AttendanceVote 변환 실패")
        }
        return vote
    }
}
```

### Task 2.4: MatchRepository.fetchDetail 추가

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Domain/Repositories/MatchRepository.swift`

- [ ] **Step 1: 프로토콜에 메서드 추가**

기존 `MatchRepository` 프로토콜 끝에 다음 메서드를 추가 (closing brace 직전):

```swift
    /// 경기 상세 1건 조회.
    func fetchDetail(matchId: Int64) async throws -> Match
```

수정 후 전체 프로토콜 모양:

```swift
public protocol MatchRepository: Sendable {
    /// 팀의 경기 목록 (월 단위 조회 지원).
    func fetchMatches(teamId: Int64, from: Date?, to: Date?) async throws -> [Match]

    /// 경기 등록 (주장/매니저만).
    func createMatch(teamId: Int64, request: CreateMatchRequest) async throws -> Match

    /// 경기 상세 1건 조회.
    func fetchDetail(matchId: Int64) async throws -> Match
}
```

### Task 2.5: MatchRepositoryImpl.fetchDetail 구현 추가

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Data/Repositories/MatchRepositoryImpl.swift`

- [ ] **Step 1: 메서드 추가**

기존 `MatchRepositoryImpl` struct 의 `createMatch` 구현 다음에 다음 메서드를 추가:

```swift
    public func fetchDetail(matchId: Int64) async throws -> Match {
        let endpoint = APIEndpoint(path: "/api/v1/matches/\(matchId)")
        let dto: MatchDTO = try await client.request(endpoint)
        guard let match = dto.toDomain() else {
            throw APIError.decoding("MatchDTO → Match 변환 실패")
        }
        return match
    }
```

### Task 2.6: M2 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Data/DTOs/AttendanceDTO.swift \
        dugout-ios/Features/Match/Sources/Data/DTOs/AttendanceRequestDTO.swift \
        dugout-ios/Features/Match/Sources/Data/Repositories/AttendanceRepositoryImpl.swift \
        dugout-ios/Features/Match/Sources/Domain/Repositories/MatchRepository.swift \
        dugout-ios/Features/Match/Sources/Data/Repositories/MatchRepositoryImpl.swift
git commit -m "feat(ios): Attendance Data 레이어 + Match 상세 fetch (Phase 3 MATCH-B 2/6)"
```

---

## Milestone 3 — MATCH-3 경기 상세 화면 골격

> 이 milestone 의 화면은 응답 변경 CTA 가 있지만 sheet 는 아직 연결하지 않음. `presentVoteSheet` 토글만 동작 (sheet 본체는 M5 에서 연결). `toast` 는 M3 에서 완성. AttendanceVoteSheet 본체가 없으므로 M3 빌드 시 sheet 호출 코드는 임시 stub 으로 둔다.

### Task 3.1: MatchDetailViewModel

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  MatchDetailViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork
import DugoutDesignSystem

@MainActor
@Observable
public final class MatchDetailViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded(MatchDetail)
        case failed(String)
    }

    public struct MatchDetail: Sendable, Equatable {
        public let match: Match
        public let attendance: AttendanceSummary
    }

    public private(set) var state: State = .idle
    public var presentVoteSheet: Bool = false
    public var toast: DGToastItem? = nil

    public let matchId: Int64
    public let currentUserId: Int64
    public let isManager: Bool

    private let matchRepository: any MatchRepository
    private let attendanceRepository: any AttendanceRepository

    public init(
        matchId: Int64,
        currentUserId: Int64,
        isManager: Bool,
        matchRepository: any MatchRepository = MatchRepositoryImpl(),
        attendanceRepository: any AttendanceRepository = AttendanceRepositoryImpl()
    ) {
        self.matchId = matchId
        self.currentUserId = currentUserId
        self.isManager = isManager
        self.matchRepository = matchRepository
        self.attendanceRepository = attendanceRepository
    }

    public func load() async {
        state = .loading
        async let matchFetch = matchRepository.fetchDetail(matchId: matchId)
        async let summaryFetch = attendanceRepository.fetchSummary(matchId: matchId)
        do {
            let (match, summary) = try await (matchFetch, summaryFetch)
            state = .loaded(MatchDetail(match: match, attendance: summary))
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("경기 정보를 불러오지 못했습니다")
        }
    }

    public func tapVote() {
        guard canVote else { return }
        presentVoteSheet = true
    }

    public func tapSummary() {
        toast = DGToastItem(
            message: "출석 요약은 다음 업데이트에 제공돼요",
            kind: .info
        )
    }

    public func onVoteCompleted(_ vote: AttendanceVote) async {
        presentVoteSheet = false
        await load()
    }

    // MARK: - Derived

    public var loadedDetail: MatchDetail? {
        if case .loaded(let detail) = state { return detail }
        return nil
    }

    public var myVote: AttendanceVote? {
        loadedDetail?.attendance.myVote(userId: currentUserId)
    }

    public var canVote: Bool {
        guard let detail = loadedDetail else { return false }
        if detail.match.status == .cancelled { return false }
        if let deadline = detail.match.voteDeadline, deadline < Date() { return false }
        return true
    }

    public var voteBlockedReason: String? {
        guard let detail = loadedDetail else { return nil }
        if detail.match.status == .cancelled { return "취소된 경기예요" }
        if let deadline = detail.match.voteDeadline, deadline < Date() {
            return "투표 마감 시간이 지났어요"
        }
        return nil
    }
}
```

### Task 3.2: MatchDetailView (sheet 미연결 stub 포함)

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  MatchDetailView.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct MatchDetailView: View {
    @State private var viewModel: MatchDetailViewModel

    public init(matchId: Int64, currentUserId: Int64, isManager: Bool) {
        _viewModel = State(
            initialValue: MatchDetailViewModel(
                matchId: matchId,
                currentUserId: currentUserId,
                isManager: isManager
            )
        )
    }

    public var body: some View {
        content
            .background(DGColor.c100)
            .navigationTitle("경기 상세")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.load() }
            .dgToast(item: $viewModel.toast)
            .sheet(isPresented: $viewModel.presentVoteSheet) {
                // M5 에서 실제 AttendanceVoteSheet 연결. 현 단계는 placeholder.
                Text("AttendanceVoteSheet (M5에서 연결)")
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .idle, .loading:
            DGLoadingState(preset: .card)
                .frame(maxHeight: .infinity)
        case .failed(let message):
            DGErrorState(message: message) {
                Task { await viewModel.load() }
            }
        case .loaded(let detail):
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    matchInfoCard(detail.match)
                    myVoteCard(detail.match)
                    attendanceSummaryCard(detail.attendance)
                    if viewModel.isManager {
                        summaryButton
                    }
                }
                .padding(.horizontal, DGSpacing.lg)
                .padding(.vertical, DGSpacing.lg)
            }
        }
    }

    // MARK: - 1) 경기 정보 카드

    private func matchInfoCard(_ match: Match) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                HStack(spacing: DGSpacing.sm) {
                    DGBadge(match.dDayLabel, variant: .dDay)
                    DGBadge(match.status.koreanLabel, variant: .neutral)
                }
                Text(Self.matchDateLabel(match))
                    .dgText(.cardTitle)
                if let gather = match.gatherTime {
                    Text("집합: \(gather.displayString)")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                }
                Text("vs \(match.opponentName ?? "상대 미정")")
                    .dgText(.bodyText)
                if let ground = match.groundName {
                    Text("📍 \(ground)")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                }
                if let deadline = match.voteDeadline {
                    Text("⏱ 투표 마감: \(Self.deadlineLabel(deadline))")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                }
                if let memo = match.memo, !memo.isEmpty {
                    Text("📝 \(memo)")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c700)
                }
            }
        }
    }

    // MARK: - 2) 내 응답 카드

    private func myVoteCard(_ match: Match) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("내 응답").dgText(.cardTitle)
                if let vote = viewModel.myVote {
                    HStack(spacing: DGSpacing.xs) {
                        Text(vote.status.emoji)
                        Text(vote.status.koreanLabel).dgText(.bodyText)
                    }
                    if let reason = vote.reason, !reason.isEmpty {
                        Text("사유: \(reason)")
                            .dgText(.subText)
                            .foregroundStyle(DGColor.c500)
                    }
                    Text("응답 시각: \(Self.respondedAtLabel(vote.respondedAt))")
                        .dgText(.label)
                        .foregroundStyle(DGColor.c500)
                } else {
                    Text("아직 응답하지 않았어요")
                        .dgText(.bodyText)
                        .foregroundStyle(DGColor.c500)
                }

                if let reason = viewModel.voteBlockedReason {
                    Text(reason)
                        .dgText(.subText)
                        .foregroundStyle(DGColor.danger)
                }

                DGButton(
                    viewModel.myVote == nil ? "지금 응답하기" : "응답 변경",
                    style: .primary,
                    isEnabled: viewModel.canVote
                ) {
                    viewModel.tapVote()
                }
            }
        }
    }

    // MARK: - 3) 출석 현황 카드

    private func attendanceSummaryCard(_ summary: AttendanceSummary) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("출석 현황").dgText(.cardTitle)

                HStack(spacing: DGSpacing.md) {
                    summaryColumn(label: "참가", count: summary.count(of: .attend), tone: .primary)
                    summaryColumn(label: "불참", count: summary.count(of: .absent), tone: .danger)
                    summaryColumn(label: "미응답", count: summary.pendingCount, tone: .neutral)
                }

                HStack(spacing: DGSpacing.md) {
                    Text("미정 \(summary.count(of: .maybe))").dgText(.subText)
                    Text("·").foregroundStyle(DGColor.c500)
                    Text("늦참 \(summary.count(of: .late))").dgText(.subText)
                    Text("·").foregroundStyle(DGColor.c500)
                    Text("조퇴 \(summary.count(of: .earlyLeave))").dgText(.subText)
                }
                .foregroundStyle(DGColor.c500)

                if !summary.votes.isEmpty {
                    Divider()
                    Text("응답자 \(summary.respondedCount)명")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(summary.votes) { vote in
                            voteRow(vote)
                        }
                    }
                }
            }
        }
    }

    private enum SummaryTone { case primary, danger, neutral }

    private func summaryColumn(label: String, count: Int, tone: SummaryTone) -> some View {
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

    private func toneColor(_ tone: SummaryTone, isDim: Bool) -> Color {
        if isDim { return DGColor.c300 }
        switch tone {
        case .primary: return DGColor.p500
        case .danger: return DGColor.danger
        case .neutral: return DGColor.c700
        }
    }

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

    // MARK: - 4) 주장 전용 전체 보기 버튼

    private var summaryButton: some View {
        DGButton("전체 보기", style: .secondary) {
            viewModel.tapSummary()
        }
    }

    // MARK: - Date Formatters (static, Sendable-safe)

    private static let matchDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 (E) · a h:mm"
        return f
    }()

    private static let deadlineFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 HH:mm"
        return f
    }()

    private static let respondedAtFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 HH:mm"
        return f
    }()

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "HH:mm"
        return f
    }()

    private static func matchDateLabel(_ match: Match) -> String {
        // match.matchDate (yyyy-MM-dd) + match.matchTime 합쳐서 노출.
        var components = Calendar.koreaCalendar.dateComponents(
            [.year, .month, .day], from: match.matchDate
        )
        components.hour = match.matchTime.hour
        components.minute = match.matchTime.minute
        let date = Calendar.koreaCalendar.date(from: components) ?? match.matchDate
        return matchDateFormatter.string(from: date)
    }

    private static func deadlineLabel(_ date: Date) -> String {
        deadlineFormatter.string(from: date)
    }

    private static func respondedAtLabel(_ date: Date) -> String {
        respondedAtFormatter.string(from: date)
    }

    private static func shortTime(_ date: Date) -> String {
        timeFormatter.string(from: date)
    }
}
```

### Task 3.3: M3 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공. (MatchDetailView 는 아직 어디서도 호출되지 않으므로 dead-code 경고는 없음. SwiftUI View 는 Top-level 사용이 없어도 컴파일 가능.)

> **검증 포인트**: 만약 `dgText`, `Calendar.koreaCalendar`, `DGColor.danger`, `DGColor.p500` 등 식별자에서 에러가 발생하면 기존 `MatchListView.swift` 가 어떻게 호출하는지 비교해서 동일하게 맞춘다 (e.g. `dgText` 시그니처 확인).

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchDetailViewModel.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift
git commit -m "feat(ios): MATCH-3 경기 상세 화면 골격 (Phase 3 MATCH-B 3/6)"
```

---

## Milestone 4 — MATCH-4 출석 응답 시트

### Task 4.1: AttendanceVoteViewModel

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/AttendanceVoteViewModel.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceVoteViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class AttendanceVoteViewModel {
    public enum State: Sendable, Equatable {
        case editing
        case submitting
        case success(AttendanceVote)
        case failed(String)
    }

    public enum MainChoice: Sendable, Equatable {
        case attend
        case absent
        case maybe
    }

    public enum PartialChoice: Sendable, Equatable {
        case none
        case late
        case earlyLeave
    }

    public private(set) var state: State = .editing
    public var mainChoice: MainChoice = .attend {
        didSet {
            // 메인을 .attend 외로 변경 시 부분 선택은 강제 해제
            if mainChoice != .attend { partialChoice = .none }
        }
    }
    public var partialChoice: PartialChoice = .none
    public var reason: String = ""

    public let matchId: Int64
    private let existingVote: AttendanceVote?
    private let repository: any AttendanceRepository

    public init(
        matchId: Int64,
        existingVote: AttendanceVote?,
        repository: any AttendanceRepository = AttendanceRepositoryImpl()
    ) {
        self.matchId = matchId
        self.existingVote = existingVote
        self.repository = repository
        if let existingVote {
            let (main, partial) = Self.decompose(existingVote.status)
            self.mainChoice = main
            self.partialChoice = partial
            self.reason = existingVote.reason ?? ""
        }
    }

    public var isUpdate: Bool { existingVote != nil }

    public var resolvedStatus: AttendanceStatus {
        switch mainChoice {
        case .absent: return .absent
        case .maybe: return .maybe
        case .attend:
            switch partialChoice {
            case .none: return .attend
            case .late: return .late
            case .earlyLeave: return .earlyLeave
            }
        }
    }

    public var canSubmit: Bool {
        if case .submitting = state { return false }
        if reason.count > 200 { return false }
        return true
    }

    public var partialEnabled: Bool {
        mainChoice == .attend
    }

    public func submit() async {
        state = .submitting
        let trimmed = reason.trimmingCharacters(in: .whitespaces)
        let request = AttendanceVoteRequest(
            status: resolvedStatus,
            reason: trimmed.isEmpty ? nil : trimmed
        )
        do {
            let vote: AttendanceVote
            if existingVote != nil {
                vote = try await repository.updateVote(matchId: matchId, request: request)
            } else {
                vote = try await postWithRetry(request: request)
            }
            state = .success(vote)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("응답 저장 중 오류가 발생했습니다")
        }
    }

    /// POST 가 ALREADY_VOTED 로 실패하면 PUT 으로 투명 재시도.
    private func postWithRetry(request: AttendanceVoteRequest) async throws -> AttendanceVote {
        do {
            return try await repository.createVote(matchId: matchId, request: request)
        } catch APIError.server(let response, _) where response.code == "ALREADY_VOTED" {
            return try await repository.updateVote(matchId: matchId, request: request)
        }
    }

    private static func decompose(_ status: AttendanceStatus) -> (MainChoice, PartialChoice) {
        switch status {
        case .attend:     return (.attend, .none)
        case .late:       return (.attend, .late)
        case .earlyLeave: return (.attend, .earlyLeave)
        case .absent:     return (.absent, .none)
        case .maybe:      return (.maybe, .none)
        }
    }
}
```

### Task 4.2: AttendanceStatusButton (재사용 컴포넌트)

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceStatusButton.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceStatusButton.swift
//  DugoutMatchFeature
//
//  출석 응답 시트의 3개 메인 버튼 + 2개 부분 토글에서 공유.
//

import SwiftUI
import DugoutDesignSystem

struct AttendanceStatusButton: View {
    let title: String
    let emoji: String
    let isSelected: Bool
    let isEnabled: Bool
    let action: () -> Void

    init(
        title: String,
        emoji: String,
        isSelected: Bool,
        isEnabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.emoji = emoji
        self.isSelected = isSelected
        self.isEnabled = isEnabled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            VStack(spacing: DGSpacing.xs) {
                Text(emoji)
                    .font(.system(size: 24))
                Text(title)
                    .font(DGFont.pretendard(.semibold, size: 14))
                    .foregroundStyle(textColor)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 72)
            .background(backgroundColor)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.card)
                    .stroke(borderColor, lineWidth: isSelected ? 2 : 1)
            )
            .opacity(isEnabled ? 1 : 0.4)
        }
        .buttonStyle(DGPressStyle())
        .disabled(!isEnabled)
    }

    private var backgroundColor: Color {
        isSelected ? DGColor.p50 : DGColor.c0
    }

    private var borderColor: Color {
        isSelected ? DGColor.p500 : DGColor.c200
    }

    private var textColor: Color {
        isSelected ? DGColor.p600 : DGColor.c700
    }
}
```

> **검증 포인트**: 만약 `DGColor.p50`, `DGColor.p600`, `DGColor.c200`, `DGFont.pretendard`, `DGRadius.card` 식별자에 컴파일 에러가 발생하면 `DGBadge.swift` 와 `DGCard.swift` 가 사용하는 식별자로 대체한다 (`grep -r "DGColor\." dugout-ios/Core/DesignSystem | grep -o "DGColor\.[a-zA-Z0-9_]*" | sort -u`).

### Task 4.3: AttendanceVoteSheet

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceVoteSheet.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  AttendanceVoteSheet.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct AttendanceVoteSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel: AttendanceVoteViewModel

    private let matchTitle: String
    private let onCompleted: (AttendanceVote) -> Void

    public init(
        matchId: Int64,
        matchTitle: String,
        existingVote: AttendanceVote?,
        onCompleted: @escaping (AttendanceVote) -> Void
    ) {
        _viewModel = State(
            initialValue: AttendanceVoteViewModel(
                matchId: matchId,
                existingVote: existingVote
            )
        )
        self.matchTitle = matchTitle
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    contextCard
                    mainChoiceSection
                    partialChoiceSection
                    reasonSection
                    if case .failed(let message) = viewModel.state {
                        Text(message)
                            .dgText(.subText)
                            .foregroundStyle(DGColor.danger)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    submitButton
                }
                .padding(DGSpacing.lg)
            }
            .background(DGColor.c100)
            .navigationTitle("출석 응답")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
            }
            .onChange(of: viewModel.state) { _, newValue in
                if case .success(let vote) = newValue {
                    onCompleted(vote)
                    dismiss()
                }
            }
        }
    }

    // MARK: - 컨텍스트

    private var contextCard: some View {
        DGCard {
            HStack(spacing: DGSpacing.sm) {
                Text("⚾")
                Text(matchTitle)
                    .dgText(.bodyText)
                    .foregroundStyle(DGColor.c700)
            }
        }
    }

    // MARK: - 메인

    private var mainChoiceSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("응답").dgText(.cardTitle)
                HStack(spacing: DGSpacing.sm) {
                    AttendanceStatusButton(
                        title: AttendanceStatus.attend.koreanLabel,
                        emoji: AttendanceStatus.attend.emoji,
                        isSelected: viewModel.mainChoice == .attend
                    ) {
                        viewModel.mainChoice = .attend
                    }
                    AttendanceStatusButton(
                        title: AttendanceStatus.absent.koreanLabel,
                        emoji: AttendanceStatus.absent.emoji,
                        isSelected: viewModel.mainChoice == .absent
                    ) {
                        viewModel.mainChoice = .absent
                    }
                    AttendanceStatusButton(
                        title: AttendanceStatus.maybe.koreanLabel,
                        emoji: AttendanceStatus.maybe.emoji,
                        isSelected: viewModel.mainChoice == .maybe
                    ) {
                        viewModel.mainChoice = .maybe
                    }
                }
            }
        }
    }

    // MARK: - 부분 참여

    private var partialChoiceSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("부분 참여").dgText(.cardTitle)
                if !viewModel.partialEnabled {
                    Text("참가일 때만 선택할 수 있어요")
                        .dgText(.label)
                        .foregroundStyle(DGColor.c500)
                }
                HStack(spacing: DGSpacing.sm) {
                    AttendanceStatusButton(
                        title: AttendanceStatus.late.koreanLabel,
                        emoji: AttendanceStatus.late.emoji,
                        isSelected: viewModel.partialChoice == .late,
                        isEnabled: viewModel.partialEnabled
                    ) {
                        viewModel.partialChoice = viewModel.partialChoice == .late ? .none : .late
                    }
                    AttendanceStatusButton(
                        title: AttendanceStatus.earlyLeave.koreanLabel,
                        emoji: AttendanceStatus.earlyLeave.emoji,
                        isSelected: viewModel.partialChoice == .earlyLeave,
                        isEnabled: viewModel.partialEnabled
                    ) {
                        viewModel.partialChoice = viewModel.partialChoice == .earlyLeave ? .none : .earlyLeave
                    }
                }
            }
        }
    }

    // MARK: - 사유

    private var reasonSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("사유 (선택)").dgText(.cardTitle)
                DGTextField(
                    label: "",
                    placeholder: "예: 회식, 부상 등",
                    text: $viewModel.reason,
                    status: viewModel.reason.count > 200
                        ? .error("200자 이내로 입력해주세요")
                        : .normal
                )
            }
        }
    }

    // MARK: - 저장

    private var submitButton: some View {
        DGButton(
            "응답 저장",
            style: .primary,
            isLoading: { if case .submitting = viewModel.state { return true } else { return false } }(),
            isEnabled: viewModel.canSubmit
        ) {
            Task { await viewModel.submit() }
        }
    }
}
```

### Task 4.4: M4 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

> **검증 포인트**: `DGTextField` 의 시그니처는 `init(label:, placeholder:, text:, status:, autocapitalize:)`. label 이 필수이므로 빈 문자열로 전달. 만약 빈 문자열 시 placeholder 가 겹치면 UI 가 어색할 수 있으니 시각 확인은 M6 수동 검증에서.

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Presentation/ViewModels/AttendanceVoteViewModel.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceStatusButton.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/AttendanceVoteSheet.swift
git commit -m "feat(ios): MATCH-4 출석 응답 시트 (Phase 3 MATCH-B 4/6)"
```

---

## Milestone 5 — 상세 ↔ 응답 연결 + 리스트 네비게이션

### Task 5.1: MatchDetailView 가 AttendanceVoteSheet 호출하도록 변경

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`

- [ ] **Step 1: sheet stub 을 실제 AttendanceVoteSheet 호출로 교체**

기존 `MatchDetailView.body` 의 `.sheet(isPresented:)` 부분:

```swift
.sheet(isPresented: $viewModel.presentVoteSheet) {
    // M5 에서 실제 AttendanceVoteSheet 연결. 현 단계는 placeholder.
    Text("AttendanceVoteSheet (M5에서 연결)")
}
```

을 다음으로 교체:

```swift
.sheet(isPresented: $viewModel.presentVoteSheet) {
    if let detail = viewModel.loadedDetail {
        AttendanceVoteSheet(
            matchId: detail.match.id,
            matchTitle: Self.sheetTitle(for: detail.match),
            existingVote: viewModel.myVote
        ) { vote in
            Task { await viewModel.onVoteCompleted(vote) }
        }
    }
}
```

그리고 `MatchDetailView` 내부에 다음 helper 를 추가 (다른 static formatter 들 옆):

```swift
    private static let sheetTitleFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 (E)"
        return f
    }()

    private static func sheetTitle(for match: Match) -> String {
        let datePart = sheetTitleFormatter.string(from: match.matchDate)
        let opponent = match.opponentName ?? "상대 미정"
        return "\(datePart) vs \(opponent)"
    }
```

### Task 5.2: MatchListViewModel 에 currentUserId 추가

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchListViewModel.swift`

- [ ] **Step 1: 프로퍼티 + 생성자 파라미터 추가**

기존 `MatchListViewModel` 의 프로퍼티 영역에 추가 (`public let isManager: Bool` 직후):

```swift
    public let currentUserId: Int64
```

`init` 시그니처를 다음으로 변경:

```swift
    public init(
        teamId: Int64,
        isManager: Bool,
        currentUserId: Int64,
        repository: any MatchRepository = MatchRepositoryImpl()
    ) {
        self.teamId = teamId
        self.isManager = isManager
        self.currentUserId = currentUserId
        self.repository = repository
    }
```

> 기존 `MatchListViewModel` 의 init 정확한 형태는 파일을 열어 확인. `currentUserId` 한 줄만 추가하고 나머지는 그대로 유지.

### Task 5.3: MatchListView 가 NavigationLink + currentUserId 사용

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchListView.swift`

- [ ] **Step 1: init 시그니처에 currentUserId 추가**

기존 `init`:

```swift
public init(teamId: Int64, isManager: Bool) {
    _viewModel = State(initialValue: MatchListViewModel(teamId: teamId, isManager: isManager))
}
```

을 다음으로 교체:

```swift
public init(teamId: Int64, isManager: Bool, currentUserId: Int64) {
    _viewModel = State(
        initialValue: MatchListViewModel(
            teamId: teamId,
            isManager: isManager,
            currentUserId: currentUserId
        )
    )
}
```

- [ ] **Step 2: matchRow 를 NavigationLink 로 감싸기**

기존 `private func matchRow(_ match: Match) -> some View` 의 시그니처는 그대로 두되, `MatchListView.body` 의 `ForEach(items)` 호출부를 NavigationLink 로 변경:

기존:

```swift
ForEach(items) { match in
    matchRow(match)
}
```

을 다음으로 교체:

```swift
ForEach(items) { match in
    NavigationLink(value: match.id) {
        matchRow(match)
    }
    .buttonStyle(.plain)
}
```

- [ ] **Step 3: navigationDestination 추가**

`MatchListView.body` 의 `.task { await viewModel.load() }` 줄 위 또는 아래에 다음 modifier 추가:

```swift
.navigationDestination(for: Int64.self) { matchId in
    MatchDetailView(
        matchId: matchId,
        currentUserId: viewModel.currentUserId,
        isManager: viewModel.isManager
    )
}
```

> **검증 포인트**: `NavigationLink(value:)` + `.navigationDestination(for:)` 는 `NavigationStack` 안에서만 동작. M5 에선 빌드만 확인하고 실제 push 동작은 M6 의 `ScheduleTabHost` 수정 이후 검증.

### Task 5.4: M5 빌드 검증 + 커밋

- [ ] **Step 1: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 실패. `MatchListView(teamId:isManager:)` 호출부(=`ScheduleTabHost.swift`)가 `currentUserId` 미전달로 에러. 다음 step 에서 fix.

- [ ] **Step 2: ScheduleTabHost.swift 호출부 임시 컴파일 통과**

> ScheduleTabHost 의 본격 수정은 M6 의 task. M5 빌드 통과만 위해 임시로 `currentUserId: 0` 하드코드 후 M6 에서 정정. 또는 M5 와 M6 를 한 묶음으로 진행해 임시 코드 없이 마무리.

선택지 A — 임시 0 하드코드:

`ScheduleTabHost.swift` 의 `MatchListView(teamId:isManager:)` 호출에 `currentUserId: 0` 한 줄 추가 (다음 task 5.5 에서 즉시 fix).

선택지 B — task 5.4 step 1 의 빌드 검증 step 을 task 5.5 의 ScheduleTabHost 수정 후로 미루기. M5 commit 도 ScheduleTabHost 수정과 함께 진행.

→ **선택지 B 채택**. M5 의 빌드 검증 step 은 다음 task 5.5 직후에 한 번에 진행.

### Task 5.5: ScheduleTabHost 에 currentUserId 임시 전달 + M5 빌드 + 커밋

**Files:**
- Modify: `dugout-ios/App/Sources/ScheduleTabHost.swift`

- [ ] **Step 1: MatchListView 호출 시 currentUserId 인자 추가 (임시 0)**

기존 호출:

```swift
MatchListView(
    teamId: firstTeam.teamId,
    isManager: firstTeam.role == .captain || firstTeam.role == .manager
)
```

을 다음으로 교체:

```swift
MatchListView(
    teamId: firstTeam.teamId,
    isManager: firstTeam.role == .captain || firstTeam.role == .manager,
    currentUserId: 0   // FIXME(M6): authViewModel.currentUser?.id 로 교체
)
```

> M6 의 ScheduleTabHost 본격 수정에서 이 줄을 실제 사용자 ID 로 교체. M5 단계는 빌드 통과만 목적.

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

- [ ] **Step 3: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift \
        dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchListViewModel.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/MatchListView.swift \
        dugout-ios/App/Sources/ScheduleTabHost.swift
git commit -m "feat(ios): 상세↔응답 시트 연결 + 리스트 네비게이션 (Phase 3 MATCH-B 5/6)"
```

---

## Milestone 6 — 통합 (ScheduleTabHost + MainTabView + 검증 + 문서 + 머지)

### Task 6.1: ScheduleTabHost 에 authViewModel 주입 + NavigationStack + currentUserId

**Files:**
- Modify: `dugout-ios/App/Sources/ScheduleTabHost.swift`

- [ ] **Step 1: 전체 교체**

```swift
import SwiftUI
import DugoutCoreNetwork
import DugoutAuthFeature
import DugoutHomeFeature
import DugoutMatchFeature
import DugoutDesignSystem

struct ScheduleTabHost: View {
    @Bindable var authViewModel: AuthViewModel
    @State private var teams: [MyTeam]?
    @State private var errorMessage: String?
    private let repository: any HomeRepository = HomeRepositoryImpl()

    var body: some View {
        Group {
            if let errorMessage {
                DGErrorState(message: errorMessage) {
                    Task { await load() }
                }
            } else if let teams {
                if let firstTeam = teams.first,
                   let currentUserId = authViewModel.currentUser?.id {
                    NavigationStack {
                        MatchListView(
                            teamId: firstTeam.teamId,
                            isManager: firstTeam.role == .captain || firstTeam.role == .manager,
                            currentUserId: currentUserId
                        )
                    }
                } else {
                    DGEmptyState(
                        icon: "⚾",
                        title: "팀과 함께 시작해요",
                        message: "팀에 참가하면 경기 일정을 볼 수 있어요"
                    )
                    .background(DGColor.c100)
                }
            } else {
                DGLoadingState(preset: .list)
                    .background(DGColor.c100)
            }
        }
        .task { await load() }
    }

    private func load() async {
        errorMessage = nil
        do {
            let result = try await repository.fetchMyTeams()
            teams = result
        } catch let error as APIError {
            errorMessage = error.userMessage
        } catch {
            errorMessage = "팀 정보를 불러오지 못했습니다"
        }
    }
}
```

> 변경 사항 요약:
> - `import DugoutAuthFeature` 추가 (AuthViewModel 사용)
> - `@Bindable var authViewModel: AuthViewModel` 프로퍼티 추가
> - 팀 + currentUserId 둘 다 있을 때만 `MatchListView` 렌더, 아니면 empty state
> - `MatchListView` 를 `NavigationStack` 으로 감쌈
> - `currentUserId: 0` 하드코드 (M5 임시) 를 `authViewModel.currentUser?.id` 로 교체
> - 기존 `if let firstTeam = teams.first` 조건을 currentUserId 와 함께 검사하도록 합침

### Task 6.2: MainTabView 가 authViewModel 을 ScheduleTabHost 에 전달

**Files:**
- Modify: `dugout-ios/App/Sources/MainTabView.swift`

- [ ] **Step 1: .schedule 케이스 수정**

기존:

```swift
case .schedule:
    ScheduleTabHost()
```

을 다음으로 교체:

```swift
case .schedule:
    ScheduleTabHost(authViewModel: authViewModel)
```

### Task 6.3: 빌드 검증

- [ ] **Step 1: iOS 빌드**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

- [ ] **Step 2: 백엔드 컴파일 점검**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

Expected: 에러 0. (백엔드 변경 없으므로 baseline 유지.)

### Task 6.4: PII 로그 가드 검증

- [ ] **Step 1: nickname / reason 로깅 검사**

```bash
cd /Users/heetae/Documents/Source/Dugout
grep -rn "print.*nickname\|print.*reason\|os_log.*nickname\|os_log.*reason" \
  dugout-ios/Features/Match/Sources/
```

Expected: 출력 0행. 만약 발견되면 해당 줄을 제거하거나 마스킹 처리 후 재실행.

### Task 6.5: docs/TDD.md 갱신

**Files:**
- Modify: `docs/TDD.md`

- [ ] **Step 1: 기존 iOS 모듈 표 위치 확인**

```bash
grep -n "DugoutMatchFeature\|iOS 모듈" /Users/heetae/Documents/Source/Dugout/docs/TDD.md | head -10
```

- [ ] **Step 2: Attendance 행 추가**

위 grep 결과에서 발견된 `DugoutMatchFeature` 행 또는 모듈 표 영역을 찾아 다음 문장을 인근에 추가 (이미 DugoutMatchFeature 행이 있다면 "책임" 컬럼을 다음으로 갱신):

> `DugoutMatchFeature`: 경기 일정(MATCH-1)·등록(MATCH-2)·상세(MATCH-3)·출석 응답(MATCH-4)을 제공한다. 백엔드 `/api/v1/teams/{teamId}/matches`, `/api/v1/matches/{matchId}`, `/api/v1/matches/{matchId}/attendance` 엔드포인트를 사용. Attendance 도메인은 모듈 내부 `Sources/Attendance/` (X — 본 모듈은 폴더 격리만, 별도 타겟 아님)로 격리. 출석 요약(MATCH-5)과 카카오 공유는 Phase 3-C 에서 추가.

> 정확한 표 형식이 있다면 한 행으로 추가, 단락 형식이라면 한 문단으로 정리. 위 문장 그대로 복붙하지 말고 기존 표 스타일에 맞춰 자연스럽게 통합.

### Task 6.6: 시뮬레이터 수동 시나리오 검증

- [ ] **Step 1: 백엔드 로컬 기동**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew bootRun --args='--spring.profiles.active=local'
```

별도 터미널에서 health check:

```bash
curl -s -w '\nHTTP %{http_code}\n' http://localhost:8080/api/v1/health
```

Expected: `HTTP 200`.

- [ ] **Step 2: 시뮬레이터에서 시나리오 통과**

Xcode 또는 `xcrun simctl` 로 시뮬레이터 실행 후 다음 8개 시나리오 모두 통과 확인. 각 결과를 메모해 commit 메시지에 포함.

1. 로그인 → 일정 탭 → 경기 카드 탭 → MatchDetailView 진입
2. 정보 헤더(D-Day, 날짜, 상대팀, 구장, 메모) 정상 노출
3. 응답 안 한 상태 → "지금 응답하기" CTA → sheet 진입
4. 메인 "참가" + 부분 해제 → "응답 저장" → sheet dismiss → 본인 응답 "참가 ✅", ATTEND 카운트 +1
5. "응답 변경" → "참가" + "늦참" 토글 + 사유 입력 → 저장 → 본인 응답 "늦참 ⏰", 사유 표시
6. "응답 변경" → "불참" → 부분 토글 자동 비활성 → 저장 → 본인 응답 "불참 ❌"
7. 주장 계정 진입 → "전체 보기" 노출, 탭 시 토스트 "출석 요약은 다음 업데이트에 제공돼요" 3초 후 자동 사라짐
8. 일반 멤버 계정 → "전체 보기" 비표시

> 시나리오 9 (voteDeadline 과거 경기 → CTA 비활성) 와 10 (CANCELLED 경기 → CTA 비활성) 은 시뮬레이터에서 만들기 어려우므로 **코드 리뷰로 대체**: `MatchDetailViewModel.canVote`, `voteBlockedReason` 의 분기 로직이 spec 의 규칙과 일치하는지 확인.

### Task 6.7: 통합 커밋 + main 머지

- [ ] **Step 1: 변경 검토**

```bash
cd /Users/heetae/Documents/Source/Dugout
git status
git diff --stat HEAD~1
```

- [ ] **Step 2: 최종 커밋 (M6 분 단독)**

```bash
git add dugout-ios/App/Sources/ScheduleTabHost.swift \
        dugout-ios/App/Sources/MainTabView.swift \
        docs/TDD.md
git commit -m "feat(ios): MainTabView↔ScheduleTabHost authViewModel 주입 + 문서 (Phase 3 MATCH-B 6/6)"
```

- [ ] **Step 3: main 으로 no-ff 머지**

```bash
git checkout main
git merge --no-ff feature/phase3-match-b -m "Merge branch 'feature/phase3-match-b'"
```

Expected: fast-forward 가 아닌 머지 커밋이 생성됨.

- [ ] **Step 4: 머지 후 상태 점검**

```bash
git log --oneline -10
```

Expected: 6개 feat 커밋 + 1개 merge 커밋이 새로 추가됨.

> 브랜치 삭제 (`git branch -d feature/phase3-match-b`) 는 사용자 권한으로 실행. 자동 삭제하지 않음.

---

## 검증 체크리스트 (Phase 3 MATCH-B 완료 조건)

- [ ] `xcodebuild -workspace Dugout.xcworkspace -scheme Dugout -configuration Debug -destination 'generic/platform=iOS Simulator' -quiet build` 성공 (warnings 0)
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공
- [ ] M6 의 수동 시나리오 1~8 통과
- [ ] 시나리오 9~10 의 코드 분기(`canVote`, `voteBlockedReason`)가 spec 과 일치
- [ ] Swift 6 Sendable 위반 0 (모든 새 타입 `Sendable`)
- [ ] `docs/TDD.md` Attendance 행 추가됨
- [ ] PII 로그 노출 없음 (`grep -r "print.*\(nickname\|reason\)" dugout-ios/Features/Match` 빈 결과)
- [ ] 새 ErrorCode 추가 없음 (기존 백엔드 enum 재사용)
- [ ] 도메인 용어 준수 (Attendance, Vote, Match — 자유 번역·로마자 변형 없음)
- [ ] feature/phase3-match-b 의 6개 commit + main 머지 완료

---

## 추후 Phase 에서 이어질 작업 (스코프 외)

- **Phase 3-C (MATCH-5)**: 출석 요약 본 화면 (주장 전용) + 카카오 공유
- **별도 보강**: AI 예측 카드(dugout-ai 연동), 경기 편집/삭제 화면, 투표 마감 임박 푸시 알림
