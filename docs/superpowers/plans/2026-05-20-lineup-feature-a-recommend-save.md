# Phase 4-A Implementation Plan: AI 라인업 추천·저장·수정

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** iOS 에 신규 `DugoutLineupFeature` 모듈을 추가하여 주장/매니저가 AI 추천 라인업을 받고, 편집한 뒤, 저장(POST)/수정(PUT) 할 수 있게 한다. 일반 멤버는 결과를 readonly 로 조회. 백엔드 + dugout-ai 는 기 구현 (변경 0).

**Architecture:** 신규 Tuist 타겟 `DugoutLineupFeature` (Core 만 의존). `MatchDetailView` 에 라인업 진입 CTA 카드 추가 — Match → Lineup Feature 의존성 1건 허용. Clean Architecture(Data/Domain/Presentation) 패턴. `LineupViewModel` 이 조회 + AI 추천 + 편집 진입 트리거를 관리하고, `LineupEditViewModel` 이 편집·검증·저장(POST/PUT 자동 분기)을 담당. POST `LINEUP_ALREADY_EXISTS` race 발생 시 자동 PUT 재시도.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, @Observable, Tuist 4.x, DugoutCoreNetwork (Alamofire wrapper). 백엔드 변경 없음 (Spring Boot 3.4.1 + Kotlin).

---

## 0. 사전 준비 — 베이스 현황 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령

```bash
# iOS — 매 task 완료 시 실행 (M1 의 Project.swift 변경 후엔 반드시 tuist generate 필요)
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build

# 백엔드 baseline (M7 에서만)
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

> 검증 단위 = `xcodebuild -quiet build` 성공 (warnings 0). iOS Unit Test 타겟 미존재.
>
> **Link cache 에러 fallback**: `_TaskModifier2`, `SwiftUICore.tbd` 같은 link 단계 에러가 나오면 stale DerivedData 문제. `xcodebuild ... -quiet clean build` 1회 실행 후 plain build 재시도. Phase 3-B/3-C 에서 동일 패턴 검증됨.

### 베이스 브랜치

```bash
cd /Users/heetae/Documents/Source/Dugout
git checkout main
git pull origin main
git checkout -b feature/phase4-lineup-a
```

### 백엔드 응답 JSON (검증용 참고)

`GET /api/v1/matches/{matchId}/lineup` → `LineupResponse`:
```json
{
  "id": 1,
  "match_id": 1,
  "team_id": 1,
  "is_ai_generated": true,
  "is_confirmed": false,
  "confirmed_at": null,
  "entries": [
    {
      "id": 1, "user_id": 10, "nickname": "박희태",
      "position": "SS", "batting_order": 2, "is_bench": false
    }
  ]
}
```

`POST /api/v1/matches/{matchId}/lineup/recommend` (no body) → `LineupRecommendationResponse`:
```json
{
  "match_id": 1,
  "is_ai_generated": true,
  "source": "STUB",
  "entries": [
    { "user_id": 10, "position": "SS", "batting_order": 2, "is_bench": false }
  ]
}
```
> `LineupEntryPayload` 에는 `id`, `nickname` 없음 — 클라이언트가 attendees 와 조인 필요.

`POST/PUT /api/v1/matches/{matchId}/lineup` body — `SaveLineupRequest`:
```json
{
  "entries": [
    { "user_id": 10, "position": "SS", "batting_order": 2, "is_bench": false }
  ]
}
```

### 백엔드 ErrorCode 재사용

`LINEUP_NOT_FOUND`(404), `LINEUP_ALREADY_EXISTS`(409), `LINEUP_ALREADY_CONFIRMED`(400), `INSUFFICIENT_ATTENDEES`(400), `INVALID_LINEUP_POSITION`(400), `AI_SERVICE_UNAVAILABLE`(503), `NOT_TEAM_MEMBER`(403), `TEAM_ROLE_NOT_ALLOWED`(403), `MATCH_NOT_FOUND`(404).

### 재사용 / 신규 / 수정 파일

**재사용 (변경 없음)**:
- iOS Auth: `BaseballPosition` 정의 (`Features/Auth/Sources/Domain/Entities/User.swift:63`) — Lineup 모듈은 자체 정의(Feature 독립)
- iOS Match: 변경 없음. 단 `MatchDetailView` 의 진입점 카드만 M6 에서 추가
- DesignSystem 컴포넌트: `DGCard`, `DGButton`, `DGBadge`, `DGSegmentedControl`, `DGToast`, `DGEmptyState`, `DGLoadingState`, `DGErrorState`, `DGFont`, `DGSpacing`, `DGColor`, `DGRadius`
- Network: `APIClient`, `APIEndpoint`, `APIError`
- 백엔드: 전부

**신규 (iOS, 17 파일)**:
- `Project.swift` 수정 (M1)
- `Features/Lineup/Sources/Placeholder.swift` (M1, M2 에서 삭제)
- Domain (9 파일, M2):
  - `Domain/Entities/BaseballPosition.swift`
  - `Domain/Entities/Lineup.swift`
  - `Domain/Entities/LineupEntry.swift`
  - `Domain/Entities/LineupDraft.swift`
  - `Domain/Entities/LineupDraftEntry.swift`
  - `Domain/Entities/LineupRecommendation.swift`
  - `Domain/Entities/Attendee.swift`
  - `Domain/Repositories/LineupRepository.swift` (+ `SaveLineupRequest`)
  - `Domain/Repositories/AttendeeRepository.swift`
- Data (5 파일, M3):
  - `Data/DTOs/LineupDTO.swift` (+ LineupEntryDTO)
  - `Data/DTOs/LineupRecommendationDTO.swift` (+ LineupEntryPayloadDTO)
  - `Data/DTOs/LineupRequestDTO.swift`
  - `Data/DTOs/AttendeeJoinDTOs.swift` (출석자 + 멤버 식별용 minimal DTOs)
  - `Data/Repositories/LineupRepositoryImpl.swift`
  - `Data/Repositories/AttendeeRepositoryImpl.swift`
- Presentation (4 파일, M4):
  - `Presentation/ViewModels/LineupViewModel.swift`
  - `Presentation/Views/LineupView.swift`
  - `Presentation/Views/LineupDiamondView.swift`
  - `Presentation/Views/BattingOrderListView.swift`
- Presentation (3 파일, M5):
  - `Presentation/ViewModels/LineupEditViewModel.swift`
  - `Presentation/Views/LineupEditView.swift`
  - `Presentation/Views/LineupAssignSheet.swift`

**수정 (iOS)**:
- `Project.swift` (M1: lineupFeature 추가 + matchFeature/app dependencies + project.targets)
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift` (M6: lineupCard 추가 + import DugoutLineupFeature)

**수정 (문서)**:
- `docs/TDD.md` (M7: DugoutLineupFeature 모듈 추가, Match→Lineup 의존성 명시, Phase 4-A 책임 단락)

### 도메인 용어

| 한글 | 영문 식별자 |
|---|---|
| 라인업 | Lineup |
| 라인업 엔트리 | LineupEntry |
| 포지션 | BaseballPosition / Position |
| 타순 | BattingOrder |
| 벤치 | Bench / isBench |
| 출석자 | Attendee |
| 추천 | Recommendation |

---

## Milestone 1 — Tuist 타겟 신설 + 빈 모듈 빌드 통과

### Task 1.1: feature/phase4-lineup-a 브랜치 생성

- [ ] **Step 1: main clean 상태에서 브랜치 생성**

```bash
cd /Users/heetae/Documents/Source/Dugout
git status      # working tree clean 확인
git checkout main
git pull origin main
git checkout -b feature/phase4-lineup-a
git branch --show-current      # feature/phase4-lineup-a
```

### Task 1.2: Project.swift 에 lineupFeature 추가

**File**: `dugout-ios/Project.swift`

- [ ] **Step 1: lineupFeature 선언 추가**

`matchFeature` 선언 (line 84-91) 직후, App Target 영역(`// MARK: - App Target` 직전) 에 다음을 추가:

```swift
let lineupFeature = frameworkTarget(
    name: "DugoutLineupFeature",
    sourcesPath: "Features/Lineup/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
    ]
)
```

- [ ] **Step 2: matchFeature dependencies 에 lineupFeature 추가**

`let matchFeature = frameworkTarget(...)` 의 `dependencies` 배열을 다음으로 변경:

```swift
let matchFeature = frameworkTarget(
    name: "DugoutMatchFeature",
    sourcesPath: "Features/Match/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
        .target(name: "DugoutLineupFeature"),
    ]
)
```

- [ ] **Step 3: App dependencies 에 추가**

App target 의 `dependencies` 배열(line 119-124) 끝에 추가:

```swift
    dependencies: [
        .target(name: "DugoutAuthFeature"),
        .target(name: "DugoutHomeFeature"),
        .target(name: "DugoutTeamFeature"),
        .target(name: "DugoutMatchFeature"),
        .target(name: "DugoutLineupFeature"),
    ],
```

- [ ] **Step 4: project.targets 배열에 추가**

마지막 `let project = Project(...)` 의 `targets` 배열에 `lineupFeature` 추가:

```swift
let project = Project(
    name: "Dugout",
    organizationName: "Dugout",
    targets: [
        app,
        coreNetwork,
        designSystem,
        authFeature,
        homeFeature,
        teamFeature,
        matchFeature,
        lineupFeature,
    ]
)
```

### Task 1.3: Placeholder 소스 파일 생성

**File**: `dugout-ios/Features/Lineup/Sources/Placeholder.swift`

Tuist 가 빈 sourcesPath 를 허용하지 않으므로 임시 파일 1개 필요. M2 에서 실제 코드 작성 후 삭제.

- [ ] **Step 1: 파일 생성**

```swift
//
//  Placeholder.swift
//  DugoutLineupFeature
//
//  M1: Tuist 타겟이 빈 sources 를 허용하지 않으므로 임시 파일.
//  M2 에서 실제 코드 작성 시 삭제.
//

import Foundation

internal enum DugoutLineupFeaturePlaceholder {}
```

### Task 1.4: tuist generate + 빌드 + 커밋

- [ ] **Step 1: 의존성 + 프로젝트 재생성**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist install        # 신규 타겟이 추가됐으므로 install 도 안전하게 실행
tuist generate --no-open
```

- [ ] **Step 2: 빌드 검증**

```bash
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공, warnings 0.

Link cache 에러 발생 시 `clean build` 후 재실행.

- [ ] **Step 3: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Project.swift \
        dugout-ios/Features/Lineup/Sources/Placeholder.swift \
        dugout-ios/Dugout.xcodeproj
git commit -m "build(ios): DugoutLineupFeature 타겟 추가 (Phase 4-A 1/7)"
```

`Dugout.xcodeproj` 가 tuist generate 로 갱신됐으면 함께 add. 다른 변경(예: workspace 파일) 있다면 다 add.

---

## Milestone 2 — Domain 레이어

### Task 2.1: BaseballPosition enum

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/BaseballPosition.swift`

- [ ] **Step 1: 파일 작성**

```swift
//
//  BaseballPosition.swift
//  DugoutLineupFeature
//
//  AuthFeature 의 BaseballPosition 과 동일한 정의 —
//  Feature 독립 원칙에 따라 각자 소유 (TeamRole 패턴과 동일).
//

import Foundation

public enum BaseballPosition: String, Sendable, Hashable, CaseIterable, Codable {
    case pitcher          = "P"
    case catcher          = "C"
    case firstBase        = "1B"
    case secondBase       = "2B"
    case thirdBase        = "3B"
    case shortStop        = "SS"
    case leftField        = "LF"
    case centerField      = "CF"
    case rightField       = "RF"
    case designatedHitter = "DH"

    public var displayName: String {
        switch self {
        case .pitcher:           "투수"
        case .catcher:           "포수"
        case .firstBase:         "1루수"
        case .secondBase:        "2루수"
        case .thirdBase:         "3루수"
        case .shortStop:         "유격수"
        case .leftField:         "좌익수"
        case .centerField:       "중견수"
        case .rightField:        "우익수"
        case .designatedHitter:  "지명타자"
        }
    }

    public var shortName: String { rawValue }
    public var isField: Bool { self != .designatedHitter }

    /// 다이아몬드/배정 화면에 사용. DH 제외 9종.
    public static var fieldPositions: [BaseballPosition] {
        [.pitcher, .catcher, .firstBase, .secondBase, .thirdBase,
         .shortStop, .leftField, .centerField, .rightField]
    }
}
```

### Task 2.2: Lineup + LineupEntry

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/Lineup.swift`

```swift
//
//  Lineup.swift
//  DugoutLineupFeature
//
//  서버 저장 상태의 라인업.
//

import Foundation

public struct Lineup: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let matchId: Int64
    public let teamId: Int64
    public let isAiGenerated: Bool
    public let isConfirmed: Bool
    public let confirmedAt: Date?
    public let entries: [LineupEntry]

    public init(
        id: Int64,
        matchId: Int64,
        teamId: Int64,
        isAiGenerated: Bool,
        isConfirmed: Bool,
        confirmedAt: Date?,
        entries: [LineupEntry]
    ) {
        self.id = id
        self.matchId = matchId
        self.teamId = teamId
        self.isAiGenerated = isAiGenerated
        self.isConfirmed = isConfirmed
        self.confirmedAt = confirmedAt
        self.entries = entries
    }
}
```

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupEntry.swift`

```swift
//
//  LineupEntry.swift
//  DugoutLineupFeature
//
//  서버 저장된 라인업 엔트리 1건.
//

import Foundation

public struct LineupEntry: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let userId: Int64
    public let nickname: String
    public let position: BaseballPosition
    public let battingOrder: Int?    // nil = 벤치
    public let isBench: Bool

    public init(
        id: Int64,
        userId: Int64,
        nickname: String,
        position: BaseballPosition,
        battingOrder: Int?,
        isBench: Bool
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.position = position
        self.battingOrder = battingOrder
        self.isBench = isBench
    }
}
```

### Task 2.3: LineupDraft + LineupDraftEntry

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupDraftEntry.swift`

```swift
//
//  LineupDraftEntry.swift
//  DugoutLineupFeature
//
//  편집 중 라인업 엔트리. 서버 저장 전이라 id 가 없고, position/battingOrder/isBench 가 mutable.
//

import Foundation

public struct LineupDraftEntry: Sendable, Equatable, Identifiable, Hashable {
    public let id: UUID
    public let userId: Int64
    public let nickname: String
    public let jerseyNumber: Int?
    public var position: BaseballPosition
    public var battingOrder: Int?
    public var isBench: Bool

    public init(
        id: UUID = UUID(),
        userId: Int64,
        nickname: String,
        jerseyNumber: Int?,
        position: BaseballPosition,
        battingOrder: Int?,
        isBench: Bool
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.jerseyNumber = jerseyNumber
        self.position = position
        self.battingOrder = battingOrder
        self.isBench = isBench
    }
}
```

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupDraft.swift`

```swift
//
//  LineupDraft.swift
//  DugoutLineupFeature
//

import Foundation

public struct LineupDraft: Sendable, Equatable {
    public var entries: [LineupDraftEntry]

    public init(entries: [LineupDraftEntry]) {
        self.entries = entries
    }
}
```

### Task 2.4: LineupRecommendation

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupRecommendation.swift`

```swift
//
//  LineupRecommendation.swift
//  DugoutLineupFeature
//
//  POST /lineup/recommend 응답. entries 는 server-side id 없는 상태이며
//  Repository 가 attendees 와 조인하여 LineupDraft 로 변환한 결과를 담음.
//

import Foundation

public struct LineupRecommendation: Sendable, Equatable {
    public let matchId: Int64
    public let source: String         // "AI" | "STUB"
    public let isAiGenerated: Bool
    public let draft: LineupDraft

    public init(matchId: Int64, source: String, isAiGenerated: Bool, draft: LineupDraft) {
        self.matchId = matchId
        self.source = source
        self.isAiGenerated = isAiGenerated
        self.draft = draft
    }
}
```

### Task 2.5: Attendee

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/Attendee.swift`

```swift
//
//  Attendee.swift
//  DugoutLineupFeature
//
//  AttendanceSummary 의 votes(status in ATTEND/LATE) 와 TeamMember(jersey_number) 의 조인 결과.
//

import Foundation

public struct Attendee: Sendable, Equatable, Identifiable, Hashable {
    public let userId: Int64
    public let nickname: String
    public let jerseyNumber: Int?

    public var id: Int64 { userId }

    public init(userId: Int64, nickname: String, jerseyNumber: Int?) {
        self.userId = userId
        self.nickname = nickname
        self.jerseyNumber = jerseyNumber
    }
}
```

### Task 2.6: LineupRepository + SaveLineupRequest

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Repositories/LineupRepository.swift`

```swift
//
//  LineupRepository.swift
//  DugoutLineupFeature
//

import Foundation

public protocol LineupRepository: Sendable {
    /// 라인업 조회. 404/LINEUP_NOT_FOUND 는 호출 측에서 .empty 로 매핑.
    func fetchLineup(matchId: Int64) async throws -> Lineup

    /// AI 추천 호출. 백엔드는 matchId 만 받음 (출석자는 백엔드가 자체 조회).
    /// `attendees` 는 응답 entries 에 nickname/jerseyNumber 가 없으므로
    /// 클라이언트가 LineupDraft 로 변환할 때 조인용으로 전달 (Repository 가 내부 enrich).
    func recommend(matchId: Int64, attendees: [Attendee]) async throws -> LineupRecommendation

    /// 신규 저장 (POST). 기존 라인업이 있으면 LINEUP_ALREADY_EXISTS 발생.
    func saveLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup

    /// 수정 (PUT). 라인업 미존재 시 LINEUP_NOT_FOUND, 확정된 라인업은 LINEUP_ALREADY_CONFIRMED.
    func updateLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
}

public struct SaveLineupRequest: Sendable, Equatable {
    public let entries: [LineupDraftEntry]

    public init(entries: [LineupDraftEntry]) {
        self.entries = entries
    }
}
```

### Task 2.7: AttendeeRepository 프로토콜

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Repositories/AttendeeRepository.swift`

```swift
//
//  AttendeeRepository.swift
//  DugoutLineupFeature
//

import Foundation

public protocol AttendeeRepository: Sendable {
    /// 경기 출석자(ATTEND + LATE) + 등번호를 조인하여 반환.
    /// 백엔드 호출: GET /matches/{matchId}/attendance + GET /teams/{teamId}/members (async let).
    func fetchAttendees(matchId: Int64, teamId: Int64) async throws -> [Attendee]
}
```

### Task 2.8: Placeholder 삭제 + 빌드 + 커밋

- [ ] **Step 1: Placeholder.swift 삭제**

```bash
rm /Users/heetae/Documents/Source/Dugout/dugout-ios/Features/Lineup/Sources/Placeholder.swift
```

- [ ] **Step 2: tuist generate + 빌드 검증**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공, warnings 0.

- [ ] **Step 3: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Lineup/Sources/Domain/ \
        dugout-ios/Features/Lineup/Sources/Placeholder.swift \
        dugout-ios/Dugout.xcodeproj
git commit -m "feat(ios): Lineup Domain 레이어 (Phase 4-A 2/7)"
```

`Placeholder.swift` 는 삭제됐으므로 `git add` 가 삭제 변경을 stage 함.

---

## Milestone 3 — Data 레이어

### Task 3.1: LineupDTO + LineupEntryDTO

**File**: `dugout-ios/Features/Lineup/Sources/Data/DTOs/LineupDTO.swift`

```swift
//
//  LineupDTO.swift
//  DugoutLineupFeature
//
//  백엔드 LineupResponse / LineupEntryResponse 매핑.
//

import Foundation

struct LineupDTO: Decodable, Sendable {
    let id: Int64
    let matchId: Int64
    let teamId: Int64
    let isAiGenerated: Bool
    let isConfirmed: Bool
    let confirmedAt: Date?
    let entries: [LineupEntryDTO]

    enum CodingKeys: String, CodingKey {
        case id, entries
        case matchId        = "match_id"
        case teamId         = "team_id"
        case isAiGenerated  = "is_ai_generated"
        case isConfirmed    = "is_confirmed"
        case confirmedAt    = "confirmed_at"
    }

    func toDomain() -> Lineup? {
        Lineup(
            id: id,
            matchId: matchId,
            teamId: teamId,
            isAiGenerated: isAiGenerated,
            isConfirmed: isConfirmed,
            confirmedAt: confirmedAt,
            entries: entries.compactMap { $0.toDomain() }
        )
    }
}

struct LineupEntryDTO: Decodable, Sendable {
    let id: Int64
    let userId: Int64
    let nickname: String
    let position: String
    let battingOrder: Int?
    let isBench: Bool

    enum CodingKeys: String, CodingKey {
        case id, nickname, position
        case userId        = "user_id"
        case battingOrder  = "batting_order"
        case isBench       = "is_bench"
    }

    func toDomain() -> LineupEntry? {
        guard let pos = BaseballPosition(rawValue: position) else { return nil }
        return LineupEntry(
            id: id,
            userId: userId,
            nickname: nickname,
            position: pos,
            battingOrder: battingOrder,
            isBench: isBench
        )
    }
}
```

### Task 3.2: LineupRecommendationDTO + LineupEntryPayloadDTO

**File**: `dugout-ios/Features/Lineup/Sources/Data/DTOs/LineupRecommendationDTO.swift`

```swift
//
//  LineupRecommendationDTO.swift
//  DugoutLineupFeature
//

import Foundation

struct LineupRecommendationDTO: Decodable, Sendable {
    let matchId: Int64
    let isAiGenerated: Bool
    let source: String
    let entries: [LineupEntryPayloadDTO]

    enum CodingKeys: String, CodingKey {
        case source, entries
        case matchId        = "match_id"
        case isAiGenerated  = "is_ai_generated"
    }
}

struct LineupEntryPayloadDTO: Decodable, Sendable {
    let userId: Int64
    let position: String
    let battingOrder: Int?
    let isBench: Bool

    enum CodingKeys: String, CodingKey {
        case position
        case userId        = "user_id"
        case battingOrder  = "batting_order"
        case isBench       = "is_bench"
    }
}
```

### Task 3.3: LineupRequestDTO (POST/PUT body)

**File**: `dugout-ios/Features/Lineup/Sources/Data/DTOs/LineupRequestDTO.swift`

```swift
//
//  LineupRequestDTO.swift
//  DugoutLineupFeature
//

import Foundation

struct LineupEntryPayloadEncodableDTO: Encodable, Sendable {
    let userId: Int64
    let position: String
    let battingOrder: Int?
    let isBench: Bool

    enum CodingKeys: String, CodingKey {
        case position
        case userId        = "user_id"
        case battingOrder  = "batting_order"
        case isBench       = "is_bench"
    }

    init(_ entry: LineupDraftEntry) {
        self.userId = entry.userId
        self.position = entry.position.rawValue
        self.battingOrder = entry.battingOrder
        self.isBench = entry.isBench
    }
}

struct SaveLineupRequestDTO: Encodable, Sendable {
    let entries: [LineupEntryPayloadEncodableDTO]

    init(_ request: SaveLineupRequest) {
        self.entries = request.entries.map(LineupEntryPayloadEncodableDTO.init)
    }
}
```

### Task 3.4: AttendeeJoinDTOs (출석자 + 멤버 식별 minimal)

**File**: `dugout-ios/Features/Lineup/Sources/Data/DTOs/AttendeeJoinDTOs.swift`

```swift
//
//  AttendeeJoinDTOs.swift
//  DugoutLineupFeature
//
//  Lineup 모듈 자체 — Match 모듈의 동명 DTO 와 별개로 자체 minimal 정의 (Feature 독립 원칙).
//  GET /matches/{matchId}/attendance + GET /teams/{teamId}/members 응답의 필요 필드만 포함.
//

import Foundation

struct AttendanceSummaryRefDTO: Decodable, Sendable {
    let votes: [AttendanceVoteRefDTO]
}

struct AttendanceVoteRefDTO: Decodable, Sendable {
    let userId: Int64
    let nickname: String
    let status: String     // "ATTEND" | "LATE" 등

    enum CodingKeys: String, CodingKey {
        case nickname, status
        case userId = "user_id"
    }
}

struct TeamMemberRefDTO: Decodable, Sendable {
    let userId: Int64
    let nickname: String
    let jerseyNumber: Int?
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case nickname
        case userId        = "user_id"
        case jerseyNumber  = "jersey_number"
        case isActive      = "is_active"
    }
}
```

### Task 3.5: LineupRepositoryImpl

**File**: `dugout-ios/Features/Lineup/Sources/Data/Repositories/LineupRepositoryImpl.swift`

```swift
//
//  LineupRepositoryImpl.swift
//  DugoutLineupFeature
//

import Foundation
import DugoutCoreNetwork

public struct LineupRepositoryImpl: LineupRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchLineup(matchId: Int64) async throws -> Lineup {
        let endpoint = APIEndpoint(path: "/api/v1/matches/\(matchId)/lineup")
        let dto: LineupDTO = try await client.request(endpoint)
        guard let lineup = dto.toDomain() else {
            throw APIError.decoding("LineupDTO → Lineup 변환 실패")
        }
        return lineup
    }

    public func recommend(matchId: Int64, attendees: [Attendee]) async throws -> LineupRecommendation {
        // 백엔드는 matchId 만 받음 (POST body 없음, no queryItems)
        let endpoint = APIEndpoint(
            path: "/api/v1/matches/\(matchId)/lineup/recommend",
            method: .post
        )
        let dto: LineupRecommendationDTO = try await client.request(endpoint)

        // attendees 와 조인하여 draft 생성
        let attendeesByUserId = Dictionary(uniqueKeysWithValues: attendees.map { ($0.userId, $0) })
        let draftEntries: [LineupDraftEntry] = dto.entries.compactMap { payload in
            guard let pos = BaseballPosition(rawValue: payload.position) else { return nil }
            let att = attendeesByUserId[payload.userId]
            return LineupDraftEntry(
                userId: payload.userId,
                nickname: att?.nickname ?? "팀원",      // attendees 에 없는 경우 fallback
                jerseyNumber: att?.jerseyNumber,
                position: pos,
                battingOrder: payload.battingOrder,
                isBench: payload.isBench
            )
        }

        return LineupRecommendation(
            matchId: dto.matchId,
            source: dto.source,
            isAiGenerated: dto.isAiGenerated,
            draft: LineupDraft(entries: draftEntries)
        )
    }

    public func saveLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup {
        let body = SaveLineupRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/lineup",
            method: .post,
            body: body
        )
        let dto: LineupDTO = try await client.request(endpoint)
        guard let lineup = dto.toDomain() else {
            throw APIError.decoding("LineupDTO → Lineup 변환 실패")
        }
        return lineup
    }

    public func updateLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup {
        let body = SaveLineupRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/lineup",
            method: .put,
            body: body
        )
        let dto: LineupDTO = try await client.request(endpoint)
        guard let lineup = dto.toDomain() else {
            throw APIError.decoding("LineupDTO → Lineup 변환 실패")
        }
        return lineup
    }
}
```

### Task 3.6: AttendeeRepositoryImpl

**File**: `dugout-ios/Features/Lineup/Sources/Data/Repositories/AttendeeRepositoryImpl.swift`

```swift
//
//  AttendeeRepositoryImpl.swift
//  DugoutLineupFeature
//

import Foundation
import DugoutCoreNetwork

public struct AttendeeRepositoryImpl: AttendeeRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchAttendees(matchId: Int64, teamId: Int64) async throws -> [Attendee] {
        async let summaryFetch: AttendanceSummaryRefDTO = client.request(
            APIEndpoint(path: "/api/v1/matches/\(matchId)/attendance")
        )
        async let membersFetch: [TeamMemberRefDTO] = client.request(
            APIEndpoint(path: "/api/v1/teams/\(teamId)/members")
        )

        let (summary, members) = try await (summaryFetch, membersFetch)
        let activeMembersByUserId = Dictionary(
            uniqueKeysWithValues: members.filter { $0.isActive }.map { ($0.userId, $0) }
        )

        // 출석자 = status in {ATTEND, LATE}
        return summary.votes
            .filter { $0.status == "ATTEND" || $0.status == "LATE" }
            .compactMap { vote -> Attendee? in
                guard let member = activeMembersByUserId[vote.userId] else {
                    // 출석자인데 active member 가 아닌 경우 (race) 는 skip
                    return nil
                }
                return Attendee(
                    userId: vote.userId,
                    nickname: vote.nickname,   // votes 의 nickname 우선 사용
                    jerseyNumber: member.jerseyNumber
                )
            }
    }
}
```

### Task 3.7: M3 빌드 + 커밋

- [ ] **Step 1: 빌드**

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
git add dugout-ios/Features/Lineup/Sources/Data/ \
        dugout-ios/Dugout.xcodeproj
git commit -m "feat(ios): Lineup Data 레이어 (Phase 4-A 3/7)"
```

---

## Milestone 4 — LineupView 조회 화면 + LineupDiamondView

### Task 4.1: LineupViewModel

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupViewModel.swift`

```swift
//
//  LineupViewModel.swift
//  DugoutLineupFeature
//

import Foundation
import Observation
import DugoutCoreNetwork
import DugoutDesignSystem

@MainActor
@Observable
public final class LineupViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case empty
        case loaded(Lineup)
        case recommending
        case failed(String)
    }

    public enum EditSource: Sendable, Equatable {
        case empty(attendees: [Attendee])
        case recommendation(LineupRecommendation, attendees: [Attendee])
        case existing(Lineup, attendees: [Attendee])
    }

    public private(set) var state: State = .idle
    public var presentEdit: Bool = false
    public var editSource: EditSource? = nil
    public var toast: DGToastItem? = nil

    public let matchId: Int64
    public let teamId: Int64
    public let isManager: Bool
    private let lineupRepository: any LineupRepository
    private let attendeeRepository: any AttendeeRepository

    public init(
        matchId: Int64,
        teamId: Int64,
        isManager: Bool,
        lineupRepository: any LineupRepository = LineupRepositoryImpl(),
        attendeeRepository: any AttendeeRepository = AttendeeRepositoryImpl()
    ) {
        self.matchId = matchId
        self.teamId = teamId
        self.isManager = isManager
        self.lineupRepository = lineupRepository
        self.attendeeRepository = attendeeRepository
    }

    public func load() async {
        state = .loading
        do {
            let lineup = try await lineupRepository.fetchLineup(matchId: matchId)
            state = .loaded(lineup)
        } catch APIError.server(let response, _) where response.code == "LINEUP_NOT_FOUND" {
            state = .empty
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("라인업을 불러오지 못했습니다")
        }
    }

    public func tapRecommend() async {
        let previousState = state
        state = .recommending
        do {
            let attendees = try await attendeeRepository.fetchAttendees(
                matchId: matchId, teamId: teamId
            )
            let recommendation = try await lineupRepository.recommend(
                matchId: matchId, attendees: attendees
            )
            editSource = .recommendation(recommendation, attendees: attendees)
            state = previousState
            presentEdit = true
        } catch APIError.server(let response, _) where response.code == "INSUFFICIENT_ATTENDEES" {
            state = previousState
            toast = DGToastItem(message: "출석자가 9명 미만이에요", kind: .warning)
        } catch APIError.server(let response, _) where response.code == "AI_SERVICE_UNAVAILABLE" {
            state = previousState
            toast = DGToastItem(
                message: "AI 서비스에 일시적으로 접근할 수 없어요", kind: .danger
            )
        } catch let error as APIError {
            state = previousState
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            state = previousState
            toast = DGToastItem(message: "라인업 추천 중 오류가 발생했어요", kind: .danger)
        }
    }

    public func tapEditExisting() async {
        guard case .loaded(let lineup) = state else { return }
        do {
            let attendees = try await attendeeRepository.fetchAttendees(
                matchId: matchId, teamId: teamId
            )
            editSource = .existing(lineup, attendees: attendees)
            presentEdit = true
        } catch let error as APIError {
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            toast = DGToastItem(message: "출석자 정보를 불러오지 못했어요", kind: .danger)
        }
    }

    public func tapWriteFromScratch() async {
        do {
            let attendees = try await attendeeRepository.fetchAttendees(
                matchId: matchId, teamId: teamId
            )
            editSource = .empty(attendees: attendees)
            presentEdit = true
        } catch let error as APIError {
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            toast = DGToastItem(message: "출석자 정보를 불러오지 못했어요", kind: .danger)
        }
    }

    public func onEditCompleted(_ lineup: Lineup) {
        state = .loaded(lineup)
        presentEdit = false
        editSource = nil
    }

    public func onEditCancelled() {
        presentEdit = false
        editSource = nil
    }

    public var hasExistingLineup: Bool {
        if case .loaded = state { return true }
        return false
    }
}
```

### Task 4.2: LineupDiamondView

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupDiamondView.swift`

```swift
//
//  LineupDiamondView.swift
//  DugoutLineupFeature
//
//  9 필드 포지션을 다이아몬드 비율 좌표로 배치. DH 는 다이아몬드 외부에 별도 노출.
//

import SwiftUI
import DugoutDesignSystem

struct LineupDiamondView: View {
    /// 필드 9 포지션의 entry. nil 이면 비어있는 자리.
    let entriesByPosition: [BaseballPosition: PositionOccupant]

    struct PositionOccupant: Sendable, Equatable {
        let nickname: String
        let jerseyNumber: Int?
    }

    /// (x, y) 비율 좌표 (0.0 ~ 1.0). 다이아몬드 위에서 본 모습.
    private static let positionCoordinates: [(BaseballPosition, x: CGFloat, y: CGFloat)] = [
        (.centerField, 0.50, 0.10),
        (.leftField,   0.20, 0.18),
        (.rightField,  0.80, 0.18),
        (.shortStop,   0.38, 0.40),
        (.secondBase,  0.62, 0.40),
        (.thirdBase,   0.22, 0.55),
        (.firstBase,   0.78, 0.55),
        (.pitcher,     0.50, 0.70),
        (.catcher,     0.50, 0.92),
    ]

    var body: some View {
        GeometryReader { geo in
            ZStack {
                background(in: geo.size)
                ForEach(Self.positionCoordinates, id: \.0) { (pos, x, y) in
                    chip(for: pos)
                        .position(x: geo.size.width * x, y: geo.size.height * y)
                }
            }
        }
        .aspectRatio(1.0, contentMode: .fit)
    }

    private func background(in size: CGSize) -> some View {
        LinearGradient(
            colors: [DGColor.p50, DGColor.c0],
            startPoint: .top, endPoint: .bottom
        )
        .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
        .overlay(
            RoundedRectangle(cornerRadius: DGRadius.card)
                .stroke(DGColor.c200, lineWidth: 1)
        )
    }

    private func chip(for position: BaseballPosition) -> some View {
        let occupant = entriesByPosition[position]
        return VStack(spacing: 2) {
            Text(position.shortName)
                .font(DGFont.pretendard(.semibold, size: 11))
                .foregroundStyle(occupant == nil ? DGColor.c500 : DGColor.p600)
            if let occupant {
                Text(label(for: occupant))
                    .font(DGFont.pretendard(.semibold, size: 12))
                    .foregroundStyle(DGColor.c900)
                    .lineLimit(1)
            } else {
                Text("—")
                    .font(DGFont.pretendard(.regular, size: 12))
                    .foregroundStyle(DGColor.c300)
            }
        }
        .frame(width: 56, height: 44)
        .background(occupant == nil ? Color.clear : DGColor.c0)
        .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
        .overlay(
            RoundedRectangle(cornerRadius: DGRadius.button)
                .stroke(
                    occupant == nil ? DGColor.c200 : DGColor.p500,
                    style: StrokeStyle(lineWidth: 1, dash: occupant == nil ? [3, 3] : [])
                )
        )
    }

    private func label(for occupant: PositionOccupant) -> String {
        // nickname 첫 2글자 + (#등번호) 또는 nickname 만
        let short = String(occupant.nickname.prefix(2))
        if let jersey = occupant.jerseyNumber {
            return "\(short) #\(jersey)"
        }
        return short
    }
}
```

### Task 4.3: BattingOrderListView

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/BattingOrderListView.swift`

```swift
//
//  BattingOrderListView.swift
//  DugoutLineupFeature
//
//  타순 1~9 리스트. battingOrder 가 있는 entry 만 노출, order 순 정렬.
//

import SwiftUI
import DugoutDesignSystem

struct BattingOrderListView: View {
    let entries: [LineupEntry]   // battingOrder != nil 인 것만 필터링되어 들어옴

    var body: some View {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            ForEach(entries.sorted { ($0.battingOrder ?? 0) < ($1.battingOrder ?? 0) }) { entry in
                row(entry)
            }
        }
    }

    private func row(_ entry: LineupEntry) -> some View {
        HStack(spacing: DGSpacing.sm) {
            Text("\(entry.battingOrder ?? 0)")
                .font(DGFont.pretendard(.bold, size: 16))
                .foregroundStyle(DGColor.p500)
                .frame(width: 24, alignment: .leading)
            Text(entry.nickname).dgText(.bodyText)
            Text("(\(entry.position.shortName))")
                .dgText(.subText)
                .foregroundStyle(DGColor.c500)
            Spacer()
        }
    }
}
```

### Task 4.4: LineupView (메인)

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift`

```swift
//
//  LineupView.swift
//  DugoutLineupFeature
//

import SwiftUI
import DugoutDesignSystem

public struct LineupView: View {
    @State private var viewModel: LineupViewModel

    public init(matchId: Int64, teamId: Int64, isManager: Bool) {
        _viewModel = State(
            initialValue: LineupViewModel(
                matchId: matchId, teamId: teamId, isManager: isManager
            )
        )
    }

    public var body: some View {
        content
            .background(DGColor.c100)
            .navigationTitle("라인업")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.load() }
            .dgToast(item: $viewModel.toast)
            .sheet(isPresented: $viewModel.presentEdit) {
                // M5 에서 실제 LineupEditView 연결. 현 단계는 placeholder.
                Text("LineupEditView (M5에서 연결)")
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .idle, .loading, .recommending:
            DGLoadingState(preset: .card)
                .frame(maxHeight: .infinity)
        case .failed(let message):
            DGErrorState(message: message) {
                Task { await viewModel.load() }
            }
        case .empty:
            emptyView
        case .loaded(let lineup):
            loadedView(lineup)
        }
    }

    // MARK: - Empty

    @ViewBuilder
    private var emptyView: some View {
        VStack(spacing: DGSpacing.lg) {
            DGEmptyState(
                icon: "⚾",
                title: "아직 라인업이 없어요",
                message: viewModel.isManager
                    ? "AI 추천으로 자동 배정하거나 직접 작성할 수 있어요"
                    : "주장이 라인업을 등록하면 여기서 확인할 수 있어요"
            )
            .padding(.top, DGSpacing.xxl)

            if viewModel.isManager {
                VStack(spacing: DGSpacing.md) {
                    DGButton("AI 추천 받기", style: .primary) {
                        Task { await viewModel.tapRecommend() }
                    }
                    DGButton("직접 작성하기", style: .secondary) {
                        Task { await viewModel.tapWriteFromScratch() }
                    }
                }
                .padding(.horizontal, DGSpacing.lg)
            }
        }
    }

    // MARK: - Loaded

    private func loadedView(_ lineup: Lineup) -> some View {
        ScrollView {
            VStack(spacing: DGSpacing.lg) {
                statusBadges(lineup)
                diamondCard(lineup)
                battingOrderCard(lineup)
                if let dhEntry = lineup.entries.first(where: { $0.position == .designatedHitter && !$0.isBench }) {
                    dhCard(dhEntry)
                }
                let benchEntries = lineup.entries.filter { $0.isBench }
                if !benchEntries.isEmpty {
                    benchCard(benchEntries)
                }
                if viewModel.isManager && !lineup.isConfirmed {
                    managerActions
                }
            }
            .padding(.horizontal, DGSpacing.lg)
            .padding(.vertical, DGSpacing.lg)
        }
    }

    private func statusBadges(_ lineup: Lineup) -> some View {
        HStack(spacing: DGSpacing.sm) {
            DGBadge(lineup.isAiGenerated ? "AI 추천" : "수동 작성", variant: .position)
            DGBadge(lineup.isConfirmed ? "확정" : "임시", variant: .neutral)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func diamondCard(_ lineup: Lineup) -> some View {
        let fieldEntries = lineup.entries.filter { !$0.isBench && $0.position.isField }
        let entriesByPosition = Dictionary(
            uniqueKeysWithValues: fieldEntries.map { entry in
                (entry.position, LineupDiamondView.PositionOccupant(
                    nickname: entry.nickname,
                    jerseyNumber: nil  // Lineup 엔트리에는 jersey 없음 — diamond 는 nickname 만 표시
                ))
            }
        )
        return DGCard {
            LineupDiamondView(entriesByPosition: entriesByPosition)
        }
    }

    private func battingOrderCard(_ lineup: Lineup) -> some View {
        let withOrder = lineup.entries.filter { !$0.isBench && $0.battingOrder != nil }
        return DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("타순").dgText(.cardTitle)
                Divider()
                BattingOrderListView(entries: withOrder)
            }
        }
    }

    private func dhCard(_ entry: LineupEntry) -> some View {
        DGCard {
            HStack(spacing: DGSpacing.sm) {
                Text("DH")
                    .font(DGFont.pretendard(.bold, size: 14))
                    .foregroundStyle(DGColor.p500)
                Text(entry.nickname).dgText(.bodyText)
                Spacer()
            }
        }
    }

    private func benchCard(_ entries: [LineupEntry]) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                Text("벤치 \(entries.count)명")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                Text(entries.map(\.nickname).joined(separator: " · "))
                    .dgText(.bodyText)
            }
        }
    }

    private var managerActions: some View {
        VStack(spacing: DGSpacing.md) {
            DGButton("편집", style: .primary) {
                Task { await viewModel.tapEditExisting() }
            }
            DGButton("AI 다시 추천", style: .secondary) {
                Task { await viewModel.tapRecommend() }
            }
        }
    }
}
```

### Task 4.5: M4 빌드 + 커밋

- [ ] **Step 1: 빌드**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
tuist generate --no-open
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 빌드 성공. LineupView 가 어디서도 호출되지 않아도 SwiftUI View 는 컴파일 가능 (dead-code 경고 없음).

- [ ] **Step 2: 커밋**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupViewModel.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupDiamondView.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/BattingOrderListView.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift \
        dugout-ios/Dugout.xcodeproj
git commit -m "feat(ios): LineupView 조회 화면 (Phase 4-A 4/7)"
```

---

## Milestone 5 — LineupEditView 편집 + 검증 + 저장

### Task 5.1: LineupEditViewModel

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupEditViewModel.swift`

```swift
//
//  LineupEditViewModel.swift
//  DugoutLineupFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class LineupEditViewModel {
    public enum State: Sendable, Equatable {
        case editing
        case submitting
        case success(Lineup)
        case failed(String)
    }
    public enum SelectedTab: Sendable, Equatable, Hashable {
        case roster
        case battingOrder
    }

    public private(set) var state: State = .editing
    public var draft: LineupDraft
    public var selectedTab: SelectedTab = .roster
    public var presentAssignSheet: Bool = false
    public var assignTargetEntry: LineupDraftEntry? = nil

    public let matchId: Int64
    public let isUpdate: Bool
    public let showOverwriteBanner: Bool
    public let attendees: [Attendee]
    private let repository: any LineupRepository

    public init(
        matchId: Int64,
        source: LineupViewModel.EditSource,
        existingLineupExists: Bool,
        repository: any LineupRepository = LineupRepositoryImpl()
    ) {
        self.matchId = matchId
        self.repository = repository

        switch source {
        case .empty(let attendees):
            self.attendees = attendees
            self.draft = LineupDraft(entries: attendees.map {
                LineupDraftEntry(
                    userId: $0.userId,
                    nickname: $0.nickname,
                    jerseyNumber: $0.jerseyNumber,
                    position: .designatedHitter,    // unassigned 마커 — 검증에서 잡힘 (DH 자체는 1명만 허용)
                    battingOrder: nil,
                    isBench: true                    // 시작은 모두 벤치 = unassigned
                )
            })
            self.isUpdate = existingLineupExists
            self.showOverwriteBanner = false

        case .recommendation(let rec, let attendees):
            self.attendees = attendees
            self.draft = rec.draft
            self.isUpdate = existingLineupExists
            self.showOverwriteBanner = existingLineupExists

        case .existing(let lineup, let attendees):
            self.attendees = attendees
            // Lineup.entries → LineupDraftEntry, attendees 에서 jerseyNumber 보충
            let attendeesByUserId = Dictionary(
                uniqueKeysWithValues: attendees.map { ($0.userId, $0) }
            )
            self.draft = LineupDraft(entries: lineup.entries.map { entry in
                LineupDraftEntry(
                    userId: entry.userId,
                    nickname: entry.nickname,
                    jerseyNumber: attendeesByUserId[entry.userId]?.jerseyNumber,
                    position: entry.position,
                    battingOrder: entry.battingOrder,
                    isBench: entry.isBench
                )
            })
            self.isUpdate = true
            self.showOverwriteBanner = false
        }
    }

    // MARK: - Actions

    public func openAssignSheet(for entry: LineupDraftEntry) {
        assignTargetEntry = entry
        presentAssignSheet = true
    }

    public func closeAssignSheet() {
        presentAssignSheet = false
        assignTargetEntry = nil
    }

    /// AssignSheet 의 "확정" 콜백. 충돌 시 기존 entry 를 unassigned(bench) 로 자동 리셋.
    public func applyAssignment(
        userId: Int64,
        position: BaseballPosition,
        battingOrder: Int?,
        isBench: Bool
    ) {
        var newEntries = draft.entries

        if !isBench {
            // 1. 같은 필드 포지션 차지한 다른 entry → bench 로 리셋 (DH 도 single-slot)
            for i in newEntries.indices {
                if newEntries[i].userId != userId
                    && newEntries[i].position == position
                    && !newEntries[i].isBench {
                    newEntries[i].position = .designatedHitter
                    newEntries[i].battingOrder = nil
                    newEntries[i].isBench = true
                }
            }
            // 2. 같은 타순 차지한 다른 entry → battingOrder = nil
            if let order = battingOrder {
                for i in newEntries.indices {
                    if newEntries[i].userId != userId
                        && newEntries[i].battingOrder == order {
                        newEntries[i].battingOrder = nil
                    }
                }
            }
        }

        // 3. 대상 entry 갱신
        if let idx = newEntries.firstIndex(where: { $0.userId == userId }) {
            newEntries[idx].position = position
            newEntries[idx].battingOrder = isBench ? nil : battingOrder
            newEntries[idx].isBench = isBench
        }

        draft.entries = newEntries
        closeAssignSheet()
    }

    public func sendToBench(_ entry: LineupDraftEntry) {
        applyAssignment(
            userId: entry.userId,
            position: .designatedHitter,
            battingOrder: nil,
            isBench: true
        )
    }

    public func swapBattingOrder(from order1: Int, to order2: Int) {
        var newEntries = draft.entries
        let idx1 = newEntries.firstIndex { $0.battingOrder == order1 }
        let idx2 = newEntries.firstIndex { $0.battingOrder == order2 }
        if let idx1, let idx2 {
            newEntries[idx1].battingOrder = order2
            newEntries[idx2].battingOrder = order1
            draft.entries = newEntries
        }
    }

    // MARK: - Validation

    public var validationErrors: [String] {
        var errors: [String] = []
        let active = draft.entries.filter { !$0.isBench }
        let fieldEntries = active.filter { $0.position.isField }

        // 1. 필드 9 포지션 모두 채워야 함
        let occupiedFieldPositions = Set(fieldEntries.map(\.position))
        for needed in BaseballPosition.fieldPositions {
            if !occupiedFieldPositions.contains(needed) {
                errors.append("\(needed.displayName) 자리가 비어있어요")
            }
        }
        // 2. 동일 필드 포지션 중복 금지
        let positionCounts = Dictionary(grouping: fieldEntries, by: \.position)
            .mapValues(\.count)
        for (pos, n) in positionCounts where n > 1 {
            errors.append("\(pos.displayName)에 \(n)명이 배정됐어요")
        }
        // 3. DH 가 2명 이상이면 에러
        let dhCount = active.filter { $0.position == .designatedHitter }.count
        if dhCount > 1 {
            errors.append("지명타자는 1명만 배정할 수 있어요")
        }
        // 4. 타순 1~9 모두 채워야 함 (active 전원 타순 가짐)
        let orders = active.compactMap(\.battingOrder).sorted()
        if Set(orders) != Set(1...9) {
            for missing in (1...9).filter({ !orders.contains($0) }) {
                errors.append("\(missing)번 타순이 비어있어요")
            }
        }
        // 5. 동일 타순 중복
        let orderCounts = Dictionary(grouping: orders, by: { $0 }).mapValues(\.count)
        for (n, count) in orderCounts where count > 1 {
            errors.append("\(n)번 타순에 \(count)명이 배정됐어요")
        }

        return errors
    }

    public var canSubmit: Bool {
        if case .submitting = state { return false }
        return validationErrors.isEmpty
    }

    // MARK: - Submit

    public func submit() async {
        state = .submitting
        let request = SaveLineupRequest(entries: draft.entries)
        do {
            let lineup: Lineup
            if isUpdate {
                lineup = try await repository.updateLineup(matchId: matchId, request: request)
            } else {
                lineup = try await postWithRetry(request: request)
            }
            state = .success(lineup)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("라인업 저장 중 오류가 발생했습니다")
        }
    }

    private func postWithRetry(request: SaveLineupRequest) async throws -> Lineup {
        do {
            return try await repository.saveLineup(matchId: matchId, request: request)
        } catch APIError.server(let response, _) where response.code == "LINEUP_ALREADY_EXISTS" {
            return try await repository.updateLineup(matchId: matchId, request: request)
        }
    }
}
```

### Task 5.2: LineupAssignSheet

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupAssignSheet.swift`

```swift
//
//  LineupAssignSheet.swift
//  DugoutLineupFeature
//

import SwiftUI
import DugoutDesignSystem

struct LineupAssignSheet: View {
    @Environment(\.dismiss) private var dismiss

    let target: LineupDraftEntry
    let draft: LineupDraft        // 다른 entry 의 점유 상태 표시용
    let onConfirm: (BaseballPosition, Int?, Bool) -> Void

    @State private var selectedPosition: BaseballPosition?
    @State private var selectedBattingOrder: Int?
    @State private var goBench: Bool = false

    init(
        target: LineupDraftEntry,
        draft: LineupDraft,
        onConfirm: @escaping (BaseballPosition, Int?, Bool) -> Void
    ) {
        self.target = target
        self.draft = draft
        self.onConfirm = onConfirm
        if target.isBench {
            _selectedPosition = State(initialValue: nil)
            _selectedBattingOrder = State(initialValue: nil)
            _goBench = State(initialValue: false)
        } else {
            _selectedPosition = State(initialValue: target.position)
            _selectedBattingOrder = State(initialValue: target.battingOrder)
            _goBench = State(initialValue: false)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    contextCard
                    positionSection
                    battingOrderSection
                    confirmButton
                }
                .padding(DGSpacing.lg)
            }
            .background(DGColor.c100)
            .navigationTitle("\(target.nickname) 배정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
            }
        }
    }

    private var contextCard: some View {
        DGCard {
            HStack {
                Text("👤")
                Text(target.nickname).dgText(.bodyText)
                if let jersey = target.jerseyNumber {
                    Text("#\(jersey)").dgText(.subText).foregroundStyle(DGColor.c500)
                }
                Spacer()
            }
        }
    }

    private var positionSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("포지션").dgText(.cardTitle)
                let columns = Array(repeating: GridItem(.flexible(), spacing: DGSpacing.sm), count: 4)
                LazyVGrid(columns: columns, spacing: DGSpacing.sm) {
                    ForEach(BaseballPosition.allCases, id: \.self) { pos in
                        positionChip(pos)
                    }
                    benchChip
                }
            }
        }
    }

    private func positionChip(_ pos: BaseballPosition) -> some View {
        let occupant = draft.entries.first(where: {
            !$0.isBench && $0.position == pos && $0.userId != target.userId
        })
        let isOccupied = occupant != nil && pos.isField   // DH 는 단일 슬롯이지만 occupant 표시 가능
        let isSelected = !goBench && selectedPosition == pos
        return Button {
            goBench = false
            selectedPosition = pos
            if pos == .designatedHitter || pos.isField {
                // 타순은 별도 선택
            }
        } label: {
            VStack(spacing: 2) {
                Text(pos.shortName)
                    .font(DGFont.pretendard(.semibold, size: 13))
                    .foregroundStyle(isSelected ? .white : DGColor.c900)
                if isOccupied, let nick = occupant?.nickname {
                    Text("\(nick.prefix(2))님")
                        .font(DGFont.pretendard(.regular, size: 10))
                        .foregroundStyle(isSelected ? .white.opacity(0.8) : DGColor.c500)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(isSelected ? DGColor.p500 : DGColor.c0)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.button)
                    .stroke(isSelected ? DGColor.p500 : DGColor.c200, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var benchChip: some View {
        Button {
            goBench = true
            selectedPosition = nil
            selectedBattingOrder = nil
        } label: {
            VStack(spacing: 2) {
                Text("벤치")
                    .font(DGFont.pretendard(.semibold, size: 13))
                    .foregroundStyle(goBench ? .white : DGColor.c900)
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(goBench ? DGColor.c700 : DGColor.c0)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.button)
                    .stroke(goBench ? DGColor.c700 : DGColor.c200, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var battingOrderSection: some View {
        if !goBench {
            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.md) {
                    Text("타순").dgText(.cardTitle)
                    if selectedPosition == nil {
                        Text("포지션을 먼저 선택해주세요")
                            .dgText(.label)
                            .foregroundStyle(DGColor.c500)
                    }
                    let columns = Array(repeating: GridItem(.flexible(), spacing: DGSpacing.sm), count: 5)
                    LazyVGrid(columns: columns, spacing: DGSpacing.sm) {
                        ForEach(1...9, id: \.self) { order in
                            battingOrderChip(order)
                        }
                    }
                }
            }
        }
    }

    private func battingOrderChip(_ order: Int) -> some View {
        let occupant = draft.entries.first(where: {
            $0.battingOrder == order && $0.userId != target.userId && !$0.isBench
        })
        let isOccupied = occupant != nil
        let isSelected = selectedBattingOrder == order
        let isEnabled = selectedPosition != nil
        return Button {
            selectedBattingOrder = order
        } label: {
            Text("\(order)")
                .font(DGFont.pretendard(.semibold, size: 14))
                .foregroundStyle(isSelected ? .white : (isEnabled ? DGColor.c900 : DGColor.c300))
                .frame(maxWidth: .infinity, minHeight: 40)
                .background(isSelected ? DGColor.p500 : DGColor.c0)
                .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.button)
                        .stroke(
                            isSelected ? DGColor.p500
                                : (isOccupied ? DGColor.c300 : DGColor.c200),
                            lineWidth: 1
                        )
                )
                .opacity(isEnabled ? 1.0 : 0.5)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
    }

    private var confirmButton: some View {
        DGButton(
            "확정",
            style: .primary,
            isEnabled: canConfirm
        ) {
            if goBench {
                onConfirm(.designatedHitter, nil, true)
            } else if let pos = selectedPosition, let order = selectedBattingOrder {
                onConfirm(pos, order, false)
            }
            dismiss()
        }
    }

    private var canConfirm: Bool {
        if goBench { return true }
        return selectedPosition != nil && selectedBattingOrder != nil
    }
}
```

### Task 5.3: LineupEditView

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupEditView.swift`

```swift
//
//  LineupEditView.swift
//  DugoutLineupFeature
//

import SwiftUI
import DugoutDesignSystem

public struct LineupEditView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel: LineupEditViewModel
    private let onCompleted: (Lineup) -> Void

    public init(
        matchId: Int64,
        source: LineupViewModel.EditSource,
        existingLineupExists: Bool,
        onCompleted: @escaping (Lineup) -> Void
    ) {
        _viewModel = State(
            initialValue: LineupEditViewModel(
                matchId: matchId,
                source: source,
                existingLineupExists: existingLineupExists
            )
        )
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    if viewModel.showOverwriteBanner {
                        overwriteBanner
                    }
                    diamondPreview
                    tabSegment
                    if viewModel.selectedTab == .roster {
                        rosterSection
                    } else {
                        battingOrderSection
                    }
                    if !viewModel.validationErrors.isEmpty {
                        validationSection
                    }
                    if case .failed(let message) = viewModel.state {
                        Text(message)
                            .dgText(.subText)
                            .foregroundStyle(DGColor.danger)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(DGSpacing.lg)
            }
            .background(DGColor.c100)
            .navigationTitle("라인업 편집")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("저장") {
                        Task { await viewModel.submit() }
                    }
                    .disabled(!viewModel.canSubmit)
                }
            }
            .sheet(isPresented: $viewModel.presentAssignSheet) {
                if let target = viewModel.assignTargetEntry {
                    LineupAssignSheet(
                        target: target,
                        draft: viewModel.draft
                    ) { pos, order, isBench in
                        viewModel.applyAssignment(
                            userId: target.userId,
                            position: pos,
                            battingOrder: order,
                            isBench: isBench
                        )
                    }
                }
            }
            .onChange(of: viewModel.state) { _, newValue in
                if case .success(let lineup) = newValue {
                    onCompleted(lineup)
                    dismiss()
                }
            }
        }
    }

    private var overwriteBanner: some View {
        DGCard(background: DGColor.warning.opacity(0.1)) {
            HStack(spacing: DGSpacing.sm) {
                Text("⚠️")
                Text("AI 추천 결과로 채워졌어요. 저장 시 기존 라인업이 덮어쓰여집니다.")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c700)
            }
        }
    }

    private var diamondPreview: some View {
        DGCard {
            let fieldEntries = viewModel.draft.entries.filter {
                !$0.isBench && $0.position.isField
            }
            let dict = Dictionary(uniqueKeysWithValues: fieldEntries.map { entry in
                (entry.position, LineupDiamondView.PositionOccupant(
                    nickname: entry.nickname,
                    jerseyNumber: entry.jerseyNumber
                ))
            })
            LineupDiamondView(entriesByPosition: dict)
        }
    }

    private var tabSegment: some View {
        DGSegmentedControl(
            options: [LineupEditViewModel.SelectedTab.roster, .battingOrder],
            selection: $viewModel.selectedTab
        ) { tab in
            switch tab {
            case .roster: "출석자"
            case .battingOrder: "타순"
            }
        }
    }

    private var rosterSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("출석자 \(viewModel.draft.entries.count)명")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                Divider()
                VStack(alignment: .leading, spacing: DGSpacing.sm) {
                    ForEach(viewModel.draft.entries) { entry in
                        rosterRow(entry)
                    }
                }
            }
        }
    }

    private func rosterRow(_ entry: LineupDraftEntry) -> some View {
        HStack(spacing: DGSpacing.sm) {
            Text(entry.nickname).dgText(.bodyText)
            if let jersey = entry.jerseyNumber {
                Text("#\(jersey)").dgText(.subText).foregroundStyle(DGColor.c500)
            }
            Text("·").foregroundStyle(DGColor.c500)
            Text(assignmentLabel(entry))
                .dgText(.subText)
                .foregroundStyle(entry.isBench ? DGColor.c500 : DGColor.c900)
            Spacer()
            Button(entry.isBench ? "지정" : "변경") {
                viewModel.openAssignSheet(for: entry)
            }
            .font(DGFont.label)
            .foregroundStyle(DGColor.p600)
            .padding(.horizontal, DGSpacing.md)
            .padding(.vertical, DGSpacing.xs)
            .background(DGColor.p50)
            .clipShape(Capsule())
            .buttonStyle(.plain)
        }
    }

    private func assignmentLabel(_ entry: LineupDraftEntry) -> String {
        if entry.isBench { return "벤치" }
        if let order = entry.battingOrder {
            return "\(entry.position.shortName) · \(order)번"
        }
        return entry.position.displayName
    }

    private var battingOrderSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("타순")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                Divider()
                let ordered = viewModel.draft.entries
                    .filter { !$0.isBench && $0.battingOrder != nil }
                    .sorted { ($0.battingOrder ?? 0) < ($1.battingOrder ?? 0) }
                if ordered.isEmpty {
                    Text("배정된 타순이 없어요")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                } else {
                    VStack(alignment: .leading, spacing: DGSpacing.sm) {
                        ForEach(ordered) { entry in
                            battingOrderRow(entry, in: ordered)
                        }
                    }
                }
            }
        }
    }

    private func battingOrderRow(_ entry: LineupDraftEntry, in ordered: [LineupDraftEntry]) -> some View {
        let myOrder = entry.battingOrder ?? 0
        let canMoveUp = myOrder > 1 && ordered.contains(where: { ($0.battingOrder ?? 0) == myOrder - 1 })
        let canMoveDown = myOrder < 9 && ordered.contains(where: { ($0.battingOrder ?? 0) == myOrder + 1 })
        return HStack(spacing: DGSpacing.sm) {
            Text("\(myOrder)")
                .font(DGFont.pretendard(.bold, size: 16))
                .foregroundStyle(DGColor.p500)
                .frame(width: 24, alignment: .leading)
            Text(entry.nickname).dgText(.bodyText)
            Text("(\(entry.position.shortName))")
                .dgText(.subText)
                .foregroundStyle(DGColor.c500)
            Spacer()
            Button {
                viewModel.swapBattingOrder(from: myOrder, to: myOrder - 1)
            } label: {
                Image(systemName: "arrow.up")
                    .foregroundStyle(canMoveUp ? DGColor.p500 : DGColor.c300)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
            .disabled(!canMoveUp)

            Button {
                viewModel.swapBattingOrder(from: myOrder, to: myOrder + 1)
            } label: {
                Image(systemName: "arrow.down")
                    .foregroundStyle(canMoveDown ? DGColor.p500 : DGColor.c300)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
            .disabled(!canMoveDown)
        }
    }

    private var validationSection: some View {
        DGCard(background: DGColor.danger.opacity(0.08)) {
            VStack(alignment: .leading, spacing: DGSpacing.xs) {
                ForEach(viewModel.validationErrors, id: \.self) { msg in
                    HStack(spacing: DGSpacing.xs) {
                        Text("·")
                        Text(msg)
                            .dgText(.subText)
                            .foregroundStyle(DGColor.danger)
                    }
                }
            }
        }
    }
}
```

### Task 5.4: LineupView 의 sheet stub 을 실제 LineupEditView 로 교체

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift`

- [ ] **Step 1: sheet 블록 교체**

기존:

```swift
            .sheet(isPresented: $viewModel.presentEdit) {
                // M5 에서 실제 LineupEditView 연결. 현 단계는 placeholder.
                Text("LineupEditView (M5에서 연결)")
            }
```

으로 변경:

```swift
            .sheet(isPresented: $viewModel.presentEdit) {
                if let source = viewModel.editSource {
                    LineupEditView(
                        matchId: viewModel.matchId,
                        source: source,
                        existingLineupExists: viewModel.hasExistingLineup
                    ) { lineup in
                        viewModel.onEditCompleted(lineup)
                    }
                }
            }
```

### Task 5.5: M5 빌드 + 커밋

- [ ] **Step 1: 빌드**

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
git add dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupEditViewModel.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupAssignSheet.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupEditView.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift \
        dugout-ios/Dugout.xcodeproj
git commit -m "feat(ios): LineupEditView 편집·검증·저장 (Phase 4-A 5/7)"
```

---

## Milestone 6 — MATCH-3 에 라인업 진입 카드 추가

### Task 6.1: MatchDetailView 수정

**File**: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`

- [ ] **Step 1: import 추가**

파일 상단의 import 영역에 `import DugoutLineupFeature` 추가 (alphabetical order 가 있다면 그에 맞춰서):

```swift
import SwiftUI
import DugoutDesignSystem
import DugoutLineupFeature   // 신규
```

- [ ] **Step 2: lineupCard 메서드 추가**

`summaryButton` 위, `voteRow` 가 있었던 자리(M3 에서 제거) 부근에 새 메서드 추가:

```swift
private func lineupCard(_ match: Match) -> some View {
    NavigationLink {
        LineupView(
            matchId: match.id,
            teamId: match.teamId,
            isManager: viewModel.isManager
        )
    } label: {
        DGCard {
            HStack {
                VStack(alignment: .leading, spacing: DGSpacing.xs) {
                    Text("라인업").dgText(.cardTitle)
                    Text("AI 추천으로 자동 배정")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(DGColor.c500)
            }
        }
    }
    .buttonStyle(.plain)
}
```

- [ ] **Step 3: 호출부 추가**

`MatchDetailView.content` 의 `.loaded` case 안의 `ScrollView { VStack ... }` 블록에서 `attendanceSummaryCard(detail.attendance)` 다음, `if viewModel.isManager { summaryButton }` 위에 lineupCard 호출 추가:

```swift
case .loaded(let detail):
    ScrollView {
        VStack(spacing: DGSpacing.lg) {
            matchInfoCard(detail.match)
            myVoteCard(detail.match)
            attendanceSummaryCard(detail.attendance)
            lineupCard(detail.match)              // ← 신규
            if viewModel.isManager {
                summaryButton
            }
        }
        .padding(.horizontal, DGSpacing.lg)
        .padding(.vertical, DGSpacing.lg)
    }
```

### Task 6.2: M6 빌드 + 커밋

- [ ] **Step 1: 빌드**

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
git add dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift
git commit -m "feat(ios): MATCH-3 에 라인업 진입 CTA 추가 (Phase 4-A 6/7)"
```

---

## Milestone 7 — 통합 (검증 + 문서 + 머지)

> 시뮬레이터 수동 시나리오와 main 머지는 controller 가 처리. implementer 는 Task 7.1~7.4.

### Task 7.1: 빌드 + 백엔드 baseline

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-ios
xcodebuild -workspace Dugout.xcworkspace -scheme Dugout \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -quiet build
```

Expected: 성공.

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew compileKotlin compileTestKotlin --quiet
```

Expected: 에러 0.

### Task 7.2: PII 로그 가드 검증

```bash
cd /Users/heetae/Documents/Source/Dugout
grep -rEn "(print|os_log|NSLog).*(nickname|jersey|attendee|member)" \
  dugout-ios/Features/Lineup/Sources/
```

Expected: 0 lines. 발견되면 제거.

### Task 7.3: docs/TDD.md 갱신

**File**: `docs/TDD.md`

- [ ] **Step 1: 의존성 다이어그램에 lineupFeature 추가**

기존 (Phase 3-C 갱신본):

```
DugoutMatchFeature          # Phase 3 MATCH-A·B·C (일정·등록·상세·출석 응답·출석 요약)
├── DugoutCoreNetwork
└── DugoutDesignSystem
```

다음으로 교체:

```
DugoutMatchFeature          # Phase 3 MATCH-A·B·C + Phase 4-A 진입점 (라인업 카드)
├── DugoutCoreNetwork
├── DugoutDesignSystem
└── DugoutLineupFeature     # 라인업 CTA + push destination

DugoutLineupFeature         # Phase 4-A (라인업 추천·저장·수정)
├── DugoutCoreNetwork
└── DugoutDesignSystem
```

- [ ] **Step 2: Feature 책임 단락 추가**

기존 DugoutMatchFeature 단락 직후에 새 단락 추가:

> `DugoutLineupFeature`는 Phase 4-A 에서 추가된 모듈로, 백엔드 `/api/v1/matches/{matchId}/lineup`(GET/POST/PUT) 과 `/recommend`, 그리고 출석자 조인용으로 `/api/v1/matches/{matchId}/attendance` + `/api/v1/teams/{teamId}/members` 엔드포인트를 사용한다. 주장/매니저는 AI 추천(헝가리안 알고리즘 — dugout-ai) 을 받아 편집·저장하고, 일반 멤버는 결과를 readonly 로 조회한다. 라인업 확정(`/confirm`)·카드 이미지 공유(`/card`)는 Phase 4-B 예정이다.

- [ ] **Step 3: "다음 페이즈에 추가될 모듈" 목록 갱신**

기존:

```
다음 페이즈에 추가될 모듈(Lineup / Finance / Matching / Mercenary / Ground / Settings 등)
```

다음으로:

```
다음 페이즈에 추가될 모듈(Finance / Matching / Mercenary / Ground / Settings 등)
```

(Lineup 제거 — 본 Phase 에서 추가됨)

### Task 7.4: 통합 커밋

```bash
cd /Users/heetae/Documents/Source/Dugout
git status      # docs/TDD.md 1 파일만 변경 확인
git add docs/TDD.md
git commit -m "docs(tdd): Phase 4-A 반영 (DugoutLineupFeature 신설)

의존성 다이어그램에 DugoutLineupFeature 추가, Match → Lineup 의존
명시. 책임 단락에 AI 라인업 추천·저장·수정 명세 추가. '다음 페이즈
추가 모듈' 목록에서 Lineup 제거 (본 Phase 에서 추가됨)."
```

다른 파일이 unexpectedly 변경됐다면 **BLOCKED** 로 보고.

### Task 7.5: 시뮬레이터 수동 시나리오 (CONTROLLER)

- [ ] **Step 1: 백엔드 로컬 기동**

```bash
cd /Users/heetae/Documents/Source/Dugout/dugout-api
./gradlew bootRun --args='--spring.profiles.active=local'
```

별도 터미널 health check:

```bash
curl -s -w '\nHTTP %{http_code}\n' http://localhost:8080/api/v1/health
```

- [ ] **Step 2: Xcode 시뮬레이터 시나리오 1~10 통과**

(spec §9 와 동일)

### Task 7.6: main 머지 + push (CONTROLLER)

```bash
cd /Users/heetae/Documents/Source/Dugout
git checkout main
git merge --no-ff feature/phase4-lineup-a -m "Merge branch 'feature/phase4-lineup-a'

Phase 4-A 완료:
- DugoutLineupFeature 신규 모듈 (Tuist 타겟)
- LineupView 조회 화면 (다이아몬드 + 타순 + DH + 벤치)
- LineupEditView 편집 모달 (출석자/타순 탭 + AssignSheet + 검증)
- AI 추천 → 편집 화면 자동 채움 (헝가리안 알고리즘)
- POST/PUT 자동 분기 + LINEUP_ALREADY_EXISTS race 재시도
- MATCH-3 에 라인업 진입 CTA 카드 추가
- docs/TDD.md DugoutLineupFeature 모듈 추가"

git branch -d feature/phase4-lineup-a
git push origin main
```

---

## 검증 체크리스트

- [ ] `xcodebuild -quiet build` 성공, warnings 0
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공
- [ ] 수동 시나리오 1~10 통과
- [ ] 시나리오 11·12 코드 리뷰 (출석자 9명 미만 / 검증 분기)
- [ ] Swift 6 Sendable 위반 0
- [ ] PII 로그 노출 0
- [ ] 새 ErrorCode 추가 없음
- [ ] 도메인 용어 준수 (Lineup, Position, Attendee — 자유 번역 없음)
- [ ] `docs/TDD.md` 3 항목 갱신 (다이어그램 + 책임 단락 + 후속 목록)
- [ ] feature/phase4-lineup-a 의 7 commit + main 머지 + push 완료

---

## 추후 Phase

- **Phase 4-B**: 확정 + 라인업 카드 이미지 공유
- **드래그&드롭 polish**: SwiftUI Drag/Drop API
- **AI 모드 전환**: BALANCED vs COMPETITIVE 팀 설정 화면
- **좌우 타석 균형 시각화**
- **알림 트리거**: 라인업 확정 시 푸시 (3-C.2 와 합쳐서)
- **공유 모듈 추출**: TeamRole + BaseballPosition + 식별자 DTO 묶음
