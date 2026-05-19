# MATCH Feature Phase 3-A: 일정·등록 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** iOS에 `DugoutMatchFeature` 모듈을 신설하고 MATCH-1(월 캘린더 + 일정 리스트) + MATCH-2(경기 등록 폼) 두 화면을 구현해 `MainTabView`의 "일정" 탭을 placeholder → 실제 동작 화면으로 전환한다.

**Architecture:** 백엔드 `MatchController`(`/api/v1/teams/{teamId}/matches` GET·POST, `/api/v1/matches/{matchId}` GET·PUT·DELETE)는 이미 구현되어 있으므로 추가 백엔드 작업 없이 검증만 수행한다. iOS는 Feature Clean Architecture(Data/Domain/Presentation)로 새 모듈을 만들고, `selectedTeamId`를 입력으로 `MatchRepository`가 실서버를 호출한다. 캘린더는 SwiftUI 자체 그리드(7×6 cell)로 구현하며 경기가 있는 날에 `DGColor.p500` 점을 표시한다. 등록 화면은 풀스크린 modal로 띄우고 성공 시 리스트가 자동 갱신된다.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, @Observable, Tuist 4.x, Alamofire (DugoutCoreNetwork 경유), Spring Boot 3.4.1 + Kotlin (백엔드 변경 없음)

---

## 0. 사전 준비 — 코드 베이스 현황 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령

```bash
# iOS
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist install
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build

# 백엔드 로컬 실행 (실연동 검증용)
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet   # 빠른 컴파일 점검
./gradlew bootRun --args='--spring.profiles.active=local'

# Health check
curl -s -w '\nHTTP %{http_code}\n' http://localhost:8080/api/v1/health
```

> 단위 테스트 타겟이 없으므로 task 검증 = `xcodebuild -quiet build` 성공. M6 완료 시점에 시뮬레이터에서 8단계 시나리오 검증.

### 재사용 / 신규 / 수정 파일

**재사용 (변경 없음):**
- 백엔드: `MatchController.kt`, `MatchService.kt`, `Match.kt`, `MatchResponse.kt`, `CreateMatchRequest.kt`, `MatchRepository.kt`, `ErrorCode.kt`
- iOS: `DugoutCoreNetwork` (`APIClient`, `APIEndpoint`, `APIError`, `JSONCoder` — 백엔드 SNAKE_CASE 자동 매핑은 **없음**. 각 DTO가 CodingKeys로 명시), `DugoutDesignSystem` 전체

**신규 (iOS):**
- `dugout-ios/Project.swift` (수정: `DugoutMatchFeature` 타겟 추가, App·Home 의존성)
- `dugout-ios/Features/Match/Sources/Domain/Entities/Match.swift`
- `dugout-ios/Features/Match/Sources/Domain/Entities/MatchStatus.swift`
- `dugout-ios/Features/Match/Sources/Domain/Repositories/MatchRepository.swift`
- `dugout-ios/Features/Match/Sources/Data/DTOs/MatchDTO.swift`
- `dugout-ios/Features/Match/Sources/Data/DTOs/MatchRequestDTO.swift`
- `dugout-ios/Features/Match/Sources/Data/Repositories/MatchRepositoryImpl.swift`
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchListViewModel.swift`
- `dugout-ios/Features/Match/Sources/Presentation/ViewModels/CreateMatchViewModel.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchListView.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchCalendarGrid.swift`
- `dugout-ios/Features/Match/Sources/Presentation/Views/CreateMatchView.swift`

**수정 (iOS):**
- `dugout-ios/App/Sources/MainTabView.swift` (`schedule` 탭을 `MatchListView`로 교체)
- `dugout-ios/Features/Home/Sources/Data/DTOs/DashboardDTO.swift` (M6: `MatchListItemDTO`의 필드명 백엔드와 일치하도록 수정 — 별도 task)

**수정 (문서):**
- `docs/TDD.md` (M6: iOS 모듈 구조 표 갱신 — DugoutMatchFeature 행 추가)

### 백엔드 응답 JSON 예시 (검증용 참고)

`GET /api/v1/teams/1/matches?from=2026-05-01&to=2026-05-31` →

```json
[
  {
    "id": 1,
    "team_id": 1,
    "opponent_name": "베어스FC",
    "opponent_team_id": null,
    "ground_id": null,
    "ground_name": "잠실야구장",
    "match_date": "2026-05-23",
    "gather_time": "07:30:00",
    "match_time": "08:00:00",
    "vote_deadline": "2026-05-22T22:00:00",
    "status": "SCHEDULED",
    "result_home": null,
    "result_away": null,
    "memo": null,
    "created_at": "2026-05-19T09:00:00"
  }
]
```

### 도메인 용어 (`CLAUDE.md`)

| 한글 | 영문 식별자 |
|------|-------------|
| 경기 | Match |
| 출석 | Attendance |
| 구장 | Ground |
| 팀 | Team |

---

## Milestone 1 — DugoutMatchFeature Tuist 타겟 신설 (빈 모듈 빌드 통과)

### Task 1.1: Project.swift에 MatchFeature 타겟 추가

**Files:**
- Modify: `dugout-ios/Project.swift`

- [ ] **Step 1: Project.swift 수정 — matchFeature 타겟 정의 + 의존성 등록**

`teamFeature` 선언 직후에 다음 추가:

```swift
let matchFeature = frameworkTarget(
    name: "DugoutMatchFeature",
    sourcesPath: "Features/Match/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
    ]
)
```

App 타겟의 `dependencies` 배열에 `.target(name: "DugoutMatchFeature")` 추가 (`DugoutTeamFeature` 다음 줄).

Home 타겟 의존성에는 추가하지 **않는다** (MATCH-A 범위에서 Home은 변경하지 않음. M6에서 별도 fix).

`project.targets` 배열에 `matchFeature` 추가.

- [ ] **Step 2: 빈 placeholder 소스 파일 생성**

`Features/Match/Sources/Placeholder.swift`:

```swift
//
//  Placeholder.swift
//  DugoutMatchFeature
//
//  M1: Tuist 타겟이 빈 sources를 허용하지 않으므로 임시 파일.
//  M2에서 실제 코드 작성 시 삭제.
//

import Foundation

internal enum DugoutMatchFeaturePlaceholder {}
```

- [ ] **Step 3: tuist generate + 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist install
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공 (no warnings).

- [ ] **Step 4: 커밋**

```bash
git add dugout-ios/Project.swift dugout-ios/Features/Match
git commit -m "build(ios): DugoutMatchFeature 타겟 추가 (Phase 3 MATCH-A 1/6)"
```

---

## Milestone 2 — Domain Layer (Match Entity + Repository 프로토콜)

### Task 2.1: MatchStatus enum

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/MatchStatus.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchStatus.swift
//  DugoutMatchFeature
//
//  백엔드 MatchStatus enum (SCHEDULED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED) 대응.
//

import Foundation

public enum MatchStatus: String, Sendable, Codable, CaseIterable {
    case scheduled = "SCHEDULED"
    case confirmed = "CONFIRMED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case cancelled = "CANCELLED"

    public var koreanLabel: String {
        switch self {
        case .scheduled: "예정"
        case .confirmed: "확정"
        case .inProgress: "진행 중"
        case .completed: "종료"
        case .cancelled: "취소"
        }
    }
}
```

### Task 2.2: Match Domain Entity

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Entities/Match.swift`

- [ ] **Step 1: 작성**

```swift
//
//  Match.swift
//  DugoutMatchFeature
//
//  도메인 Match 엔티티. 백엔드 MatchResponse(snake_case) → 이 struct로 변환.
//

import Foundation

public struct Match: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let teamId: Int64
    public let opponentName: String?
    public let opponentTeamId: Int64?
    public let groundId: Int64?
    public let groundName: String?
    public let matchDate: Date          // 날짜만 (시:분:초 = 00:00:00, Asia/Seoul)
    public let gatherTime: TimeOfDay?
    public let matchTime: TimeOfDay
    public let voteDeadline: Date?
    public let status: MatchStatus
    public let memo: String?
    public let createdAt: Date

    public init(
        id: Int64,
        teamId: Int64,
        opponentName: String?,
        opponentTeamId: Int64?,
        groundId: Int64?,
        groundName: String?,
        matchDate: Date,
        gatherTime: TimeOfDay?,
        matchTime: TimeOfDay,
        voteDeadline: Date?,
        status: MatchStatus,
        memo: String?,
        createdAt: Date
    ) {
        self.id = id
        self.teamId = teamId
        self.opponentName = opponentName
        self.opponentTeamId = opponentTeamId
        self.groundId = groundId
        self.groundName = groundName
        self.matchDate = matchDate
        self.gatherTime = gatherTime
        self.matchTime = matchTime
        self.voteDeadline = voteDeadline
        self.status = status
        self.memo = memo
        self.createdAt = createdAt
    }

    /// matchDate + matchTime을 합친 절대 시각 (Asia/Seoul 기준).
    public var scheduledAt: Date {
        let calendar = Calendar.koreaCalendar
        var components = calendar.dateComponents([.year, .month, .day], from: matchDate)
        components.hour = matchTime.hour
        components.minute = matchTime.minute
        components.second = matchTime.second
        return calendar.date(from: components) ?? matchDate
    }

    public var dDayLabel: String {
        let calendar = Calendar.koreaCalendar
        let today = calendar.startOfDay(for: Date())
        let day = calendar.startOfDay(for: matchDate)
        let delta = calendar.dateComponents([.day], from: today, to: day).day ?? 0
        if delta == 0 { return "D-DAY" }
        if delta > 0  { return "D-\(delta)" }
        return "종료"
    }
}

/// LocalTime(HH:mm:ss) 대응 값 타입.
public struct TimeOfDay: Sendable, Equatable, Codable {
    public let hour: Int
    public let minute: Int
    public let second: Int

    public init(hour: Int, minute: Int, second: Int = 0) {
        self.hour = hour
        self.minute = minute
        self.second = second
    }

    /// "HH:mm:ss" 또는 "HH:mm" 파싱.
    public init?(string: String) {
        let parts = string.split(separator: ":").compactMap { Int($0) }
        guard parts.count >= 2, (0...23).contains(parts[0]), (0...59).contains(parts[1]) else {
            return nil
        }
        self.hour = parts[0]
        self.minute = parts[1]
        self.second = parts.count >= 3 ? parts[2] : 0
    }

    /// 백엔드 송신 포맷 "HH:mm:ss".
    public var wireString: String {
        String(format: "%02d:%02d:%02d", hour, minute, second)
    }

    /// 표시 포맷 "오후 7:30".
    public var displayString: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "a h:mm"
        var components = DateComponents()
        components.hour = hour
        components.minute = minute
        let date = Calendar.koreaCalendar.date(from: components) ?? Date()
        return formatter.string(from: date)
    }
}

extension Calendar {
    static var koreaCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        calendar.locale = Locale(identifier: "ko_KR")
        return calendar
    }
}
```

### Task 2.3: MatchRepository 프로토콜

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Domain/Repositories/MatchRepository.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchRepository.swift
//  DugoutMatchFeature
//

import Foundation

public protocol MatchRepository: Sendable {
    /// 팀의 경기 목록 (월 단위 조회 지원).
    /// - Parameters:
    ///   - teamId: 팀 ID
    ///   - from: 시작일 (포함). nil이면 전체.
    ///   - to: 종료일 (포함). nil이면 전체.
    func fetchMatches(teamId: Int64, from: Date?, to: Date?) async throws -> [Match]

    /// 경기 등록 (주장/매니저만).
    func createMatch(teamId: Int64, request: CreateMatchRequest) async throws -> Match
}

public struct CreateMatchRequest: Sendable {
    public let matchDate: Date
    public let matchTime: TimeOfDay
    public let gatherTime: TimeOfDay?
    public let opponentName: String?
    public let groundName: String?
    public let voteDeadline: Date?
    public let memo: String?

    public init(
        matchDate: Date,
        matchTime: TimeOfDay,
        gatherTime: TimeOfDay? = nil,
        opponentName: String? = nil,
        groundName: String? = nil,
        voteDeadline: Date? = nil,
        memo: String? = nil
    ) {
        self.matchDate = matchDate
        self.matchTime = matchTime
        self.gatherTime = gatherTime
        self.opponentName = opponentName
        self.groundName = groundName
        self.voteDeadline = voteDeadline
        self.memo = memo
    }
}
```

- [ ] **Step 2: Placeholder.swift 삭제**

```bash
rm /Users/heetae/Documents/Source/Dugout/dugout-ios/Features/Match/Sources/Placeholder.swift
```

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

- [ ] **Step 4: 커밋**

```bash
git add dugout-ios/Features/Match/Sources/Domain dugout-ios/Features/Match/Sources/Placeholder.swift
git commit -m "feat(ios): DugoutMatchFeature Domain 레이어 (Phase 3 MATCH-A 2/6)"
```

---

## Milestone 3 — Data Layer (DTO + Repository 구현체)

### Task 3.1: MatchDTO (응답)

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/DTOs/MatchDTO.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchDTO.swift
//  DugoutMatchFeature
//
//  백엔드 MatchResponse 매핑.
//  Spring Boot Jackson SNAKE_CASE 글로벌 설정이므로 CodingKeys로 명시 매핑.
//

import Foundation

struct MatchDTO: Decodable, Sendable {
    let id: Int64
    let teamId: Int64
    let opponentName: String?
    let opponentTeamId: Int64?
    let groundId: Int64?
    let groundName: String?
    let matchDate: String      // "yyyy-MM-dd" (LocalDate)
    let gatherTime: String?    // "HH:mm:ss" (LocalTime)
    let matchTime: String      // "HH:mm:ss"
    let voteDeadline: Date?    // LocalDateTime ISO8601
    let status: String
    let resultHome: Int?
    let resultAway: Int?
    let memo: String?
    let createdAt: Date

    enum CodingKeys: String, CodingKey {
        case id
        case teamId         = "team_id"
        case opponentName   = "opponent_name"
        case opponentTeamId = "opponent_team_id"
        case groundId       = "ground_id"
        case groundName     = "ground_name"
        case matchDate      = "match_date"
        case gatherTime     = "gather_time"
        case matchTime      = "match_time"
        case voteDeadline   = "vote_deadline"
        case status
        case resultHome     = "result_home"
        case resultAway     = "result_away"
        case memo
        case createdAt      = "created_at"
    }

    func toDomain() -> Match? {
        guard let date = LocalDateFormatter.shared.date(from: matchDate),
              let mTime = TimeOfDay(string: matchTime) else {
            return nil
        }
        let gTime = gatherTime.flatMap(TimeOfDay.init(string:))
        let parsedStatus = MatchStatus(rawValue: status) ?? .scheduled
        return Match(
            id: id,
            teamId: teamId,
            opponentName: opponentName,
            opponentTeamId: opponentTeamId,
            groundId: groundId,
            groundName: groundName,
            matchDate: date,
            gatherTime: gTime,
            matchTime: mTime,
            voteDeadline: voteDeadline,
            status: parsedStatus,
            memo: memo,
            createdAt: createdAt
        )
    }
}

enum LocalDateFormatter {
    nonisolated(unsafe) static let shared: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "Asia/Seoul")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}
```

### Task 3.2: CreateMatchRequestDTO

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/DTOs/MatchRequestDTO.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchRequestDTO.swift
//  DugoutMatchFeature
//

import Foundation

struct CreateMatchRequestDTO: Encodable, Sendable {
    let matchDate: String       // "yyyy-MM-dd"
    let matchTime: String       // "HH:mm:ss"
    let gatherTime: String?
    let opponentName: String?
    let opponentTeamId: Int64?
    let groundId: Int64?
    let groundName: String?
    let voteDeadline: Date?
    let memo: String?

    enum CodingKeys: String, CodingKey {
        case matchDate      = "match_date"
        case matchTime      = "match_time"
        case gatherTime     = "gather_time"
        case opponentName   = "opponent_name"
        case opponentTeamId = "opponent_team_id"
        case groundId       = "ground_id"
        case groundName     = "ground_name"
        case voteDeadline   = "vote_deadline"
        case memo
    }
}
```

### Task 3.3: MatchRepositoryImpl

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Data/Repositories/MatchRepositoryImpl.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchRepositoryImpl.swift
//  DugoutMatchFeature
//

import Foundation
import DugoutCoreNetwork

public struct MatchRepositoryImpl: MatchRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchMatches(teamId: Int64, from: Date?, to: Date?) async throws -> [Match] {
        var queryItems: [URLQueryItem] = []
        if let from {
            queryItems.append(.init(name: "from", value: LocalDateFormatter.shared.string(from: from)))
        }
        if let to {
            queryItems.append(.init(name: "to", value: LocalDateFormatter.shared.string(from: to)))
        }
        let endpoint = APIEndpoint(
            path: "/api/v1/teams/\(teamId)/matches",
            queryItems: queryItems
        )
        let dtos: [MatchDTO] = try await client.request(endpoint)
        return dtos.compactMap { $0.toDomain() }
    }

    public func createMatch(teamId: Int64, request: CreateMatchRequest) async throws -> Match {
        let body = CreateMatchRequestDTO(
            matchDate: LocalDateFormatter.shared.string(from: request.matchDate),
            matchTime: request.matchTime.wireString,
            gatherTime: request.gatherTime?.wireString,
            opponentName: request.opponentName,
            opponentTeamId: nil,
            groundId: nil,
            groundName: request.groundName,
            voteDeadline: request.voteDeadline,
            memo: request.memo
        )
        let endpoint = APIEndpoint.json(
            path: "/api/v1/teams/\(teamId)/matches",
            method: .post,
            body: body
        )
        let dto: MatchDTO = try await client.request(endpoint)
        guard let match = dto.toDomain() else {
            throw APIError.decoding("MatchDTO → Match 변환 실패")
        }
        return match
    }
}
```

- [ ] **Step 2: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공. `compactMap`이 invalid date를 drop하므로 잘못된 응답에도 crash 없음.

- [ ] **Step 3: 커밋**

```bash
git add dugout-ios/Features/Match/Sources/Data
git commit -m "feat(ios): Match Data 레이어 (DTO + Repository 구현체) (Phase 3 MATCH-A 3/6)"
```

---

## Milestone 4 — MATCH-1 화면 (월 캘린더 + 일정 리스트)

### Task 4.1: MatchListViewModel

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/MatchListViewModel.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchListViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class MatchListViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded([Match])
        case failed(String)
    }

    public private(set) var state: State = .idle
    public private(set) var displayedMonth: Date = Date()
    public var selectedDate: Date? = nil
    public var presentCreateSheet: Bool = false

    public let teamId: Int64
    public let isManager: Bool
    private let repository: any MatchRepository

    public init(
        teamId: Int64,
        isManager: Bool,
        repository: any MatchRepository = MatchRepositoryImpl()
    ) {
        self.teamId = teamId
        self.isManager = isManager
        self.repository = repository
    }

    public func load() async {
        state = .loading
        let (from, to) = monthRange(for: displayedMonth)
        do {
            let matches = try await repository.fetchMatches(teamId: teamId, from: from, to: to)
            state = .loaded(matches)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("일정을 불러오지 못했습니다")
        }
    }

    public func goPreviousMonth() async {
        displayedMonth = shifted(months: -1)
        selectedDate = nil
        await load()
    }

    public func goNextMonth() async {
        displayedMonth = shifted(months: 1)
        selectedDate = nil
        await load()
    }

    public func tapCreate() {
        presentCreateSheet = true
    }

    public func onCreated(_ match: Match) async {
        presentCreateSheet = false
        await load()
    }

    public var monthLabel: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy년 M월"
        return formatter.string(from: displayedMonth)
    }

    public var matches: [Match] {
        if case .loaded(let list) = state { return list }
        return []
    }

    public var filteredMatches: [Match] {
        guard let selectedDate else { return matches }
        let calendar = Calendar.koreaCalendar
        return matches.filter {
            calendar.isDate($0.matchDate, inSameDayAs: selectedDate)
        }
    }

    public func hasMatch(on date: Date) -> Bool {
        let calendar = Calendar.koreaCalendar
        return matches.contains { calendar.isDate($0.matchDate, inSameDayAs: date) }
    }

    // MARK: - Helpers

    private func monthRange(for date: Date) -> (Date, Date) {
        let calendar = Calendar.koreaCalendar
        let comps = calendar.dateComponents([.year, .month], from: date)
        let start = calendar.date(from: comps) ?? date
        let endComps = DateComponents(month: 1, day: -1)
        let end = calendar.date(byAdding: endComps, to: start) ?? date
        return (start, end)
    }

    private func shifted(months delta: Int) -> Date {
        Calendar.koreaCalendar.date(byAdding: .month, value: delta, to: displayedMonth) ?? displayedMonth
    }
}
```

### Task 4.2: MatchCalendarGrid (7×6 그리드 View)

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchCalendarGrid.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchCalendarGrid.swift
//  DugoutMatchFeature
//
//  월간 7x6 그리드. 경기 있는 날 p500 점 표시, 선택 시 p500 원 배경.
//

import SwiftUI
import DugoutDesignSystem

struct MatchCalendarGrid: View {
    let displayedMonth: Date
    let selectedDate: Date?
    let hasMatch: (Date) -> Bool
    let onSelect: (Date) -> Void

    private let calendar = Calendar.koreaCalendar
    private let weekdaySymbols = ["일", "월", "화", "수", "목", "금", "토"]

    var body: some View {
        VStack(spacing: DGSpacing.sm) {
            weekdayHeader
            grid
        }
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(weekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .dgText(.caption)
                    .foregroundStyle(DGColor.c500)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var grid: some View {
        let dates = monthDates()
        let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 7)

        return LazyVGrid(columns: columns, spacing: DGSpacing.sm) {
            ForEach(Array(dates.enumerated()), id: \.offset) { _, date in
                if let date {
                    dayCell(for: date)
                } else {
                    Color.clear.frame(height: 40)
                }
            }
        }
    }

    private func dayCell(for date: Date) -> some View {
        let isSelected = selectedDate.map { calendar.isDate($0, inSameDayAs: date) } ?? false
        let isToday = calendar.isDateInToday(date)
        let day = calendar.component(.day, from: date)

        return Button {
            onSelect(date)
        } label: {
            VStack(spacing: 2) {
                Text("\(day)")
                    .dgText(.body)
                    .foregroundStyle(isSelected ? Color.white : (isToday ? DGColor.p500 : DGColor.c700))
                    .frame(width: 32, height: 32)
                    .background(
                        Circle()
                            .fill(isSelected ? DGColor.p500 : Color.clear)
                    )
                Circle()
                    .fill(hasMatch(date) ? DGColor.p500 : Color.clear)
                    .frame(width: 4, height: 4)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /// 해당 월의 1일을 시작으로 7x6 그리드용 nil padding 포함 날짜 배열을 반환.
    private func monthDates() -> [Date?] {
        guard
            let monthStart = calendar.date(
                from: calendar.dateComponents([.year, .month], from: displayedMonth)
            ),
            let dayRange = calendar.range(of: .day, in: .month, for: monthStart)
        else {
            return []
        }
        // 일요일=1 ... 토요일=7 → 그리드 인덱스 0..6
        let firstWeekday = calendar.component(.weekday, from: monthStart) - 1
        var cells: [Date?] = Array(repeating: nil, count: firstWeekday)
        for day in dayRange {
            if let date = calendar.date(byAdding: .day, value: day - 1, to: monthStart) {
                cells.append(date)
            }
        }
        // 6주 그리드(42칸)로 패딩
        while cells.count < 42 { cells.append(nil) }
        return cells
    }
}
```

### Task 4.3: MatchListView (메인 화면)

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchListView.swift`

- [ ] **Step 1: 작성**

```swift
//
//  MatchListView.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct MatchListView: View {
    @State private var viewModel: MatchListViewModel

    public init(teamId: Int64, isManager: Bool) {
        _viewModel = State(initialValue: MatchListViewModel(teamId: teamId, isManager: isManager))
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    monthHeader
                    DGCard {
                        VStack(spacing: DGSpacing.md) {
                            MatchCalendarGrid(
                                displayedMonth: viewModel.displayedMonth,
                                selectedDate: viewModel.selectedDate,
                                hasMatch: viewModel.hasMatch(on:),
                                onSelect: { date in viewModel.selectedDate = date }
                            )
                        }
                    }
                    matchList
                }
                .padding(.horizontal, DGSpacing.lg)
                .padding(.vertical, DGSpacing.lg)
            }
            .background(DGColor.c100)

            if viewModel.isManager {
                fab
                    .padding(DGSpacing.xl)
            }
        }
        .task { await viewModel.load() }
        .fullScreenCover(isPresented: $viewModel.presentCreateSheet) {
            CreateMatchView(teamId: viewModel.teamId) { match in
                Task { await viewModel.onCreated(match) }
            }
        }
    }

    private var monthHeader: some View {
        HStack {
            DGButton(style: .ghost) {
                Task { await viewModel.goPreviousMonth() }
            } label: {
                Image(systemName: "chevron.left")
            }
            Spacer()
            Text(viewModel.monthLabel).dgText(.sectionTitle)
            Spacer()
            DGButton(style: .ghost) {
                Task { await viewModel.goNextMonth() }
            } label: {
                Image(systemName: "chevron.right")
            }
        }
    }

    @ViewBuilder
    private var matchList: some View {
        switch viewModel.state {
        case .idle, .loading:
            DGLoadingState()
        case .failed(let message):
            DGErrorState(message: message) {
                Task { await viewModel.load() }
            }
        case .loaded:
            let items = viewModel.filteredMatches
            if items.isEmpty {
                DGEmptyState(
                    icon: "⚾",
                    title: "예정된 경기가 없어요",
                    message: viewModel.isManager ? "+ 버튼으로 경기를 등록해보세요" : "주장이 경기를 등록하면 여기에 표시돼요"
                )
                .padding(.vertical, DGSpacing.xxl)
            } else {
                VStack(spacing: DGSpacing.md) {
                    ForEach(items) { match in
                        matchRow(match)
                    }
                }
            }
        }
    }

    private func matchRow(_ match: Match) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                HStack {
                    DGBadge(text: match.dDayLabel, style: .primary)
                    Text(matchDateLabel(match)).dgText(.subText).foregroundStyle(DGColor.c500)
                    Spacer()
                    DGBadge(text: match.status.koreanLabel, style: .neutral)
                }
                Text("vs \(match.opponentName ?? "상대 미정")").dgText(.cardTitle)
                if let ground = match.groundName {
                    Text("📍 \(ground)").dgText(.caption).foregroundStyle(DGColor.c500)
                }
            }
        }
    }

    private func matchDateLabel(_ match: Match) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "M월 d일 (E)"
        return "\(formatter.string(from: match.matchDate)) · \(match.matchTime.displayString)"
    }

    private var fab: some View {
        Button {
            viewModel.tapCreate()
        } label: {
            Image(systemName: "plus")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(DGColor.p500)
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
        }
        .accessibilityLabel("경기 등록")
    }
}
```

> **DG 컴포넌트 확인 필요:** `DGButton(style: .ghost)`, `DGBadge(text:style:)`, `DGLoadingState`, `DGErrorState`, `DGEmptyState`가 DesignSystem 모듈에 이미 존재하는지 빌드 시 검증. 없으면 가장 가까운 시그니처로 호출부를 수정 (DGStateViews.swift 참고).

- [ ] **Step 2: DG 컴포넌트 시그니처 확인**

```bash
grep -n "public struct DGButton\|public struct DGBadge\|public struct DGEmptyState\|public struct DGLoadingState\|public struct DGErrorState\|public enum.*Style" \
  /Users/heetae/Documents/Source/Dugout/dugout-ios/Core/DesignSystem/Sources/Components/*.swift
```

호출부와 일치하지 않으면 `MatchListView.swift`의 해당 호출을 실제 시그니처에 맞춰 수정한다. (예: `DGButton`이 `action:label:` 형태가 아니면 그에 맞춤)

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공. CreateMatchView 미존재로 컴파일 에러 발생 시 Task 5.1 후 재시도.

- [ ] **Step 4: 임시 CreateMatchView 스텁** (Task 5.1 전 빌드 통과용)

`Features/Match/Sources/Presentation/Views/CreateMatchView.swift`:

```swift
import SwiftUI

public struct CreateMatchView: View {
    let teamId: Int64
    let onCreated: (Match) -> Void

    public init(teamId: Int64, onCreated: @escaping (Match) -> Void) {
        self.teamId = teamId
        self.onCreated = onCreated
    }

    public var body: some View {
        Text("CreateMatchView (M5에서 구현)")
    }
}
```

- [ ] **Step 5: 빌드 재검증**

위와 동일 명령. 빌드 성공해야 함.

- [ ] **Step 6: 커밋**

```bash
git add dugout-ios/Features/Match/Sources/Presentation
git commit -m "feat(ios): MATCH-1 일정 화면 (캘린더+리스트) (Phase 3 MATCH-A 4/6)"
```

---

## Milestone 5 — MATCH-2 화면 (경기 등록 폼)

### Task 5.1: CreateMatchViewModel

**Files:**
- Create: `dugout-ios/Features/Match/Sources/Presentation/ViewModels/CreateMatchViewModel.swift`

- [ ] **Step 1: 작성**

```swift
//
//  CreateMatchViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class CreateMatchViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(Match)
        case failed(String)
    }

    public private(set) var state: State = .editing

    public var matchDate: Date = Calendar.koreaCalendar.startOfDay(for: defaultDate())
    public var matchTime: Date = defaultTimeOfDay(hour: 8)
    public var hasGatherTime: Bool = true
    public var gatherTime: Date = defaultTimeOfDay(hour: 7, minute: 30)
    public var opponentName: String = ""
    public var groundName: String = ""
    public var hasVoteDeadline: Bool = false
    public var voteDeadline: Date = defaultDeadline()
    public var memo: String = ""

    public let teamId: Int64
    private let repository: any MatchRepository

    public init(
        teamId: Int64,
        repository: any MatchRepository = MatchRepositoryImpl()
    ) {
        self.teamId = teamId
        self.repository = repository
    }

    public var canSubmit: Bool {
        if case .submitting = state { return false }
        return true   // 백엔드는 matchDate/matchTime만 required, 그 외는 모두 선택
    }

    public func submit() async {
        state = .submitting
        let trimmedOpponent = opponentName.trimmingCharacters(in: .whitespaces)
        let trimmedGround = groundName.trimmingCharacters(in: .whitespaces)
        let trimmedMemo = memo.trimmingCharacters(in: .whitespaces)

        let request = CreateMatchRequest(
            matchDate: matchDate,
            matchTime: timeOfDay(from: matchTime),
            gatherTime: hasGatherTime ? timeOfDay(from: gatherTime) : nil,
            opponentName: trimmedOpponent.isEmpty ? nil : trimmedOpponent,
            groundName: trimmedGround.isEmpty ? nil : trimmedGround,
            voteDeadline: hasVoteDeadline ? voteDeadline : nil,
            memo: trimmedMemo.isEmpty ? nil : trimmedMemo
        )

        do {
            let match = try await repository.createMatch(teamId: teamId, request: request)
            state = .success(match)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("경기 등록 중 오류가 발생했습니다")
        }
    }

    private func timeOfDay(from date: Date) -> TimeOfDay {
        let comps = Calendar.koreaCalendar.dateComponents([.hour, .minute], from: date)
        return TimeOfDay(hour: comps.hour ?? 0, minute: comps.minute ?? 0)
    }

    private static func defaultDate() -> Date {
        Calendar.koreaCalendar.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    }

    private static func defaultTimeOfDay(hour: Int, minute: Int = 0) -> Date {
        var comps = Calendar.koreaCalendar.dateComponents([.year, .month, .day], from: Date())
        comps.hour = hour
        comps.minute = minute
        return Calendar.koreaCalendar.date(from: comps) ?? Date()
    }

    private static func defaultDeadline() -> Date {
        Calendar.koreaCalendar.date(byAdding: .day, value: 6, to: Date()) ?? Date()
    }
}
```

### Task 5.2: CreateMatchView (실제 구현)

**Files:**
- Modify: `dugout-ios/Features/Match/Sources/Presentation/Views/CreateMatchView.swift` (Task 4.4 스텁 → 실제 구현)

- [ ] **Step 1: 작성 (전체 교체)**

```swift
//
//  CreateMatchView.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct CreateMatchView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel: CreateMatchViewModel
    private let onCreated: (Match) -> Void

    public init(teamId: Int64, onCreated: @escaping (Match) -> Void) {
        _viewModel = State(initialValue: CreateMatchViewModel(teamId: teamId))
        self.onCreated = onCreated
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    section("날짜 / 시간") {
                        DatePicker("경기 날짜", selection: $viewModel.matchDate, displayedComponents: .date)
                            .environment(\.locale, Locale(identifier: "ko_KR"))
                        DatePicker("경기 시간", selection: $viewModel.matchTime, displayedComponents: .hourAndMinute)
                            .environment(\.locale, Locale(identifier: "ko_KR"))
                        Toggle("집합 시간 분리", isOn: $viewModel.hasGatherTime)
                        if viewModel.hasGatherTime {
                            DatePicker("집합 시간", selection: $viewModel.gatherTime, displayedComponents: .hourAndMinute)
                                .environment(\.locale, Locale(identifier: "ko_KR"))
                        }
                    }
                    section("상대 / 장소") {
                        DGTextField(placeholder: "상대팀 (선택)", text: $viewModel.opponentName)
                        DGTextField(placeholder: "구장명 (선택)", text: $viewModel.groundName)
                    }
                    section("출석 마감") {
                        Toggle("마감 시간 설정", isOn: $viewModel.hasVoteDeadline)
                        if viewModel.hasVoteDeadline {
                            DatePicker("마감 시각", selection: $viewModel.voteDeadline)
                                .environment(\.locale, Locale(identifier: "ko_KR"))
                        }
                    }
                    section("메모") {
                        DGTextField(placeholder: "메모 (선택)", text: $viewModel.memo)
                    }
                    if case .failed(let message) = viewModel.state {
                        Text(message)
                            .dgText(.subText)
                            .foregroundStyle(DGColor.danger)
                    }
                    submitButton
                }
                .padding(DGSpacing.lg)
            }
            .background(DGColor.c100)
            .navigationTitle("경기 등록")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
            }
            .onChange(of: viewModel.state) { _, newValue in
                if case .success(let match) = newValue {
                    onCreated(match)
                    dismiss()
                }
            }
        }
    }

    private func section<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text(title).dgText(.cardTitle)
                content()
            }
        }
    }

    private var submitButton: some View {
        DGButton(style: .primary) {
            Task { await viewModel.submit() }
        } label: {
            if case .submitting = viewModel.state {
                ProgressView().tint(.white)
            } else {
                Text("경기 등록")
            }
        }
        .disabled(!viewModel.canSubmit)
    }
}

extension CreateMatchViewModel.State: Equatable {
    public static func == (lhs: CreateMatchViewModel.State, rhs: CreateMatchViewModel.State) -> Bool {
        switch (lhs, rhs) {
        case (.editing, .editing), (.submitting, .submitting): true
        case (.success(let a), .success(let b)): a.id == b.id
        case (.failed(let a), .failed(let b)): a == b
        default: false
        }
    }
}
```

- [ ] **Step 2: DG 컴포넌트 시그니처 재확인**

`DGTextField(placeholder:text:)`, `DGButton(style:action:label:)` 시그니처가 실제와 다르면 호출부 조정. 특히 DesignSystem의 `DGButton` 구현을 확인:

```bash
sed -n '1,40p' /Users/heetae/Documents/Source/Dugout/dugout-ios/Core/DesignSystem/Sources/Components/DGButton.swift
sed -n '1,40p' /Users/heetae/Documents/Source/Dugout/dugout-ios/Core/DesignSystem/Sources/Components/DGTextField.swift
```

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공. Swift 6 Sendable 위반 시 `@Sendable` 클로저 캡처 점검.

- [ ] **Step 4: 커밋**

```bash
git add dugout-ios/Features/Match/Sources/Presentation/ViewModels/CreateMatchViewModel.swift \
        dugout-ios/Features/Match/Sources/Presentation/Views/CreateMatchView.swift
git commit -m "feat(ios): MATCH-2 경기 등록 폼 (Phase 3 MATCH-A 5/6)"
```

---

## Milestone 6 — 통합 (MainTabView + Home DTO fix + 백엔드 검증 + 문서)

### Task 6.1: MainTabView의 schedule 탭 와이어업

**Files:**
- Modify: `dugout-ios/App/Sources/MainTabView.swift`

- [ ] **Step 1: import 추가 및 placeholder 교체**

`import DugoutHomeFeature` 줄 아래에 추가:

```swift
import DugoutMatchFeature
```

`content` 스위치의 `.schedule` case를 다음으로 교체:

```swift
case .schedule:
    scheduleContent
```

private helper 추가 (placeholder 아래):

```swift
@ViewBuilder
private var scheduleContent: some View {
    if let teamId = authViewModel.currentTeamContext?.teamId,
       let role = authViewModel.currentTeamContext?.role {
        MatchListView(
            teamId: teamId,
            isManager: role == .captain || role == .manager
        )
    } else {
        DGEmptyState(
            icon: "⚾",
            title: "팀과 함께 시작해요",
            message: "팀에 참가하면 경기 일정을 볼 수 있어요"
        )
        .background(DGColor.c100)
    }
}
```

> **선결 조건:** `AuthViewModel`에 `currentTeamContext: (teamId: Int64, role: TeamRole)?` 형태의 헬퍼가 없다면, 가장 가까운 방식으로 대체:
> - `authViewModel.user?.teamId` 등 기존 프로퍼티 확인 후 그대로 사용
> - 없으면 `MatchListView(teamId: 1, isManager: true)` 하드코딩 + `// BACKEND-GAP: 팀 컨텍스트 주입 필요` 주석 (다음 Phase에서 보강)

확인:
```bash
grep -n "currentTeamContext\|currentTeam\|selectedTeam\|teamId" \
  /Users/heetae/Documents/Source/Dugout/dugout-ios/Features/Auth/Sources/Presentation/ViewModels/AuthViewModel.swift
```

- [ ] **Step 2: Project.swift의 app 의존성에 DugoutMatchFeature 포함 확인**

이미 Task 1.1에서 처리됨. 누락 시 추가:

```swift
.target(name: "DugoutMatchFeature"),
```

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

### Task 6.2: Home의 MatchListItemDTO 백엔드 불일치 fix

**Why:** 기존 `DashboardDTO.swift`의 `MatchListItemDTO`는 `match_id`, `scheduled_at`, `address` 필드를 기대하지만 백엔드 `MatchResponse`는 `id`, `match_date`, `match_time`을 보내고 `address` 필드는 존재하지 않는다. 현재 dashboard "다음 경기" 카드는 런타임에 decoding error로 항상 fallback 됨.

**Files:**
- Modify: `dugout-ios/Features/Home/Sources/Data/DTOs/DashboardDTO.swift`

- [ ] **Step 1: MatchListItemDTO 필드 교정**

```swift
struct MatchListItemDTO: Decodable, Sendable {
    let id: Int64
    let opponentName: String?
    let groundName: String?
    let matchDate: String   // "yyyy-MM-dd"
    let matchTime: String   // "HH:mm:ss"
    let status: String?

    enum CodingKeys: String, CodingKey {
        case id
        case opponentName = "opponent_name"
        case groundName   = "ground_name"
        case matchDate    = "match_date"
        case matchTime    = "match_time"
        case status
    }

    func toNextMatch() -> NextMatch? {
        guard let scheduledAt = combine(date: matchDate, time: matchTime) else { return nil }
        return NextMatch(
            id: id,
            opponentName: opponentName,
            scheduledAt: scheduledAt,
            groundName: groundName,
            address: nil   // BACKEND-GAP: Match 엔티티에 주소 없음. Ground 도메인 연동 시 보강
        )
    }

    private func combine(date: String, time: String) -> Date? {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "Asia/Seoul")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.date(from: "\(date) \(time)")
    }
}
```

- [ ] **Step 2: HomeRepositoryImpl의 toNextMatch 사용처 교정**

`HomeRepositoryImpl.swift:55-60`의 `.map { $0.toNextMatch() }`를 `.compactMap { $0.toNextMatch() }`로 변경 (Optional 반환에 대응).

- [ ] **Step 3: 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공.

### Task 6.3: 백엔드 실연동 검증 (수동)

- [ ] **Step 1: 백엔드 컴파일 점검**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

Expected: 에러 0.

- [ ] **Step 2: 로컬 서버 기동**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- [ ] **Step 3: 시뮬레이터에서 시나리오 수동 검증**

다른 터미널에서 시뮬레이터 실행 후 다음 시나리오 통과:

1. 앱 진입 → 로그인 → 온보딩 완료 → 팀 생성
2. 하단 탭 "일정" 선택 → MatchListView 진입
3. 월간 캘린더 표시 + 이전/다음 달 버튼 동작
4. 우하단 FAB(`+`) 탭 → CreateMatchView 진입 (주장 권한)
5. 날짜·시간·구장 입력 후 "경기 등록" → 성공 토스트 없이 dismiss + 리스트 갱신
6. 등록한 경기가 캘린더 점 + 리스트 카드로 표시
7. 일반 멤버 계정으로 재로그인 시 FAB 비표시
8. 다른 팀에서 등록 시도 → 403 에러 메시지 노출

각 단계의 결과를 PR 본문 또는 commit 메시지에 기록.

### Task 6.4: 문서 갱신

**Files:**
- Modify: `docs/TDD.md` (iOS 모듈 구조 표가 있는 섹션)

- [ ] **Step 1: DugoutMatchFeature 행 추가**

iOS 모듈 표(있다면)에 다음 행 추가:

| 모듈 | 의존성 | 책임 |
|------|--------|------|
| DugoutMatchFeature | CoreNetwork, DesignSystem | 경기 일정·등록·상세·출석 (Phase 3) |

표가 없다면 "iOS 모듈" 섹션을 찾아 다음 단락 추가:

> `DugoutMatchFeature`는 Phase 3 MATCH-A에서 추가된 모듈로, 백엔드 `/api/v1/teams/{teamId}/matches`와 `/api/v1/matches/{matchId}` 엔드포인트를 사용한다. 캘린더 + 일정 리스트(MATCH-1)와 등록 폼(MATCH-2)을 제공하며 출석 응답·요약은 Phase 3-B, 3-C에서 추가될 예정이다.

### Task 6.5: 최종 커밋 + 머지

- [ ] **Step 1: 변경분 검토**

```bash
git status
git diff --stat
```

- [ ] **Step 2: 통합 커밋**

```bash
git add dugout-ios/App/Sources/MainTabView.swift \
        dugout-ios/Features/Home/Sources/Data/DTOs/DashboardDTO.swift \
        dugout-ios/Features/Home/Sources/Data/Repositories/HomeRepositoryImpl.swift \
        docs/TDD.md
git commit -m "feat(ios): MainTabView 일정 탭 와이어업 + Home DTO 백엔드 정합 (Phase 3 MATCH-A 6/6)"
```

- [ ] **Step 3: PR 또는 머지 결정**

Phase 1/2 패턴(피처 브랜치 → main 머지)을 따라:
- 브랜치명: `feature/phase3-match-a`
- 머지 메시지: `feat(ios): Phase 3 MATCH-A — 일정(MATCH-1) + 등록(MATCH-2) 구현`
- 머지 후 워크트리/브랜치 정리:

```bash
git checkout main
git merge --no-ff feature/phase3-match-a
# 워크트리/브랜치 정리는 사용자 권한으로 실행 필요
```

---

## 검증 체크리스트 (Phase 3 MATCH-A 완료 조건)

- [ ] `xcodebuild -quiet build` 성공 (warnings 0)
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공
- [ ] Task 6.3의 8단계 수동 시나리오 모두 통과
- [ ] `docs/TDD.md` 갱신 커밋 포함
- [ ] PII / 크리덴셜 로그 노출 없음 (사용자명·전화번호 등 로깅 금지)
- [ ] 새 ErrorCode 추가 없음 (백엔드는 기존 enum 재사용)
- [ ] 도메인 용어 준수: Match, Attendance, Ground (자유 번역 없음)

## 추후 Phase에서 이어질 작업 (스코프 외)

- **Phase 3-B**: MATCH-3(경기 상세) + MATCH-4(출석 응답) — Attendance Domain·UI
- **Phase 3-C**: MATCH-5(출석 요약, 주장 전용) + 카카오 공유
- **별도 보강**: 백엔드 `recurring_rule` 처리 (반복 일정 자동 생성), `vote_deadline` 도달 시 status를 CONFIRMED로 자동 전이하는 스케줄러, Ground 도메인과 매칭(주소·좌표 표시)
