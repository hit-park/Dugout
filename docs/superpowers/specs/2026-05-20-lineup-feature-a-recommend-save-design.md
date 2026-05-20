# Phase 4-A: AI 라인업 추천·저장·수정 Design

**작성일**: 2026-05-20
**상태**: 설계 합의 완료, implementation plan 대기
**선행**: Phase 3 (Match·Attendance) — main 머지 완료. 백엔드 Lineup 도메인 + dugout-ai 헝가리안 알고리즘 기 구현.

---

## 1. 목표

iOS 에 신규 `DugoutLineupFeature` 모듈을 추가하여 주장/매니저가 (1) 출석자 기반 AI 라인업 추천을 받고, (2) 직접 편집(포지션·타순·벤치) 한 뒤, (3) 저장/수정할 수 있게 한다. 일반 멤버는 결과를 읽기 전용으로 조회.

> **디자인 가이드 부재**: `docs/design/flows/` 에 라인업 화면 명세가 없음 (TEAM 플로우 메모에 "라인업·회비는 v0.5+로 dim" 으로만 표기). 본 spec 이 화면 정의의 단일 출처. 후속 polish 단계에서 디자인 가이드 보충 가능.

---

## 2. 스코프

### 포함
- 신규 Tuist 타겟 `DugoutLineupFeature` (Match/Auth/Team/Home 과 같은 레벨, Core 만 의존)
- `LineupView` 조회 화면 — 다이아몬드 도식 + 타순 리스트 + DH/벤치 보조
- `LineupEditView` 편집 모달 — 출석자 배정 + 타순 정렬 + 벤치 처리
- AI 추천 호출 + 결과를 편집 화면에 자동 채움
- POST(신규 저장) / PUT(수정) 분기 + `LINEUP_ALREADY_EXISTS` race 자동 PUT 재시도
- MATCH-3 `MatchDetailView` 에 라인업 진입 CTA 카드 추가
- Match → Lineup Feature 의존성 명시 (모듈 다이어그램 갱신)

### 제외 (4-B 또는 후속)
- 라인업 확정 (`POST /lineup/confirm`)
- 라인업 카드 이미지 공유 (`GET /lineup/card`)
- 드래그&드롭 (4-A 는 탭+선택 모달)
- BALANCED/COMPETITIVE 팀 라인업 모드 전환 UI (백엔드는 팀 설정 그대로 사용)
- 좌우 타석 균형 시각화
- AI 추천 결과 비교 (기존 vs 새 추천)
- 알림 트리거

### 백엔드 변경
**없음**. 다음 기존 endpoint 사용:
- `GET /api/v1/matches/{matchId}/lineup` → `LineupResponse`
- `POST /api/v1/matches/{matchId}/lineup/recommend` → `LineupRecommendationResponse`
- `POST /api/v1/matches/{matchId}/lineup` (Body: `SaveLineupRequest`) → 201 `LineupResponse`
- `PUT /api/v1/matches/{matchId}/lineup` (Body: 동일) → 200 `LineupResponse`
- `GET /api/v1/matches/{matchId}/attendance` → `AttendanceSummaryResponse` (출석자 식별)
- `GET /api/v1/teams/{teamId}/members` → `[TeamMemberResponse]` (등번호 조인)

---

## 3. 모듈 구조 & 파일 배치

```
dugout-ios/Features/Lineup/Sources/
├── Domain/
│   ├── Entities/
│   │   ├── BaseballPosition.swift              (10종 — Auth 모듈 정의와 동일 패턴, 자체 소유)
│   │   ├── Lineup.swift                          (서버 상태)
│   │   ├── LineupEntry.swift                     (서버 entry)
│   │   ├── LineupDraft.swift                     (편집 중 상태)
│   │   ├── LineupDraftEntry.swift                (편집 중 entry — UUID id)
│   │   ├── LineupRecommendation.swift            (recommend 응답)
│   │   └── Attendee.swift                        (출석자 + 등번호 조인 결과)
│   └── Repositories/
│       ├── LineupRepository.swift                (4 메서드)
│       └── AttendeeRepository.swift              (출석자 + 멤버 조인)
├── Data/
│   ├── DTOs/
│   │   ├── LineupDTO.swift                       (LineupResponse + LineupEntryResponse 매핑)
│   │   ├── LineupRecommendationDTO.swift         (LineupRecommendationResponse 매핑)
│   │   ├── LineupRequestDTO.swift                (SaveLineupRequest + LineupEntryPayload)
│   │   ├── AttendanceSummaryRefDTO.swift         (출석자 식별용 minimal — match 모듈과 별개)
│   │   └── TeamMemberRefDTO.swift                (등번호 조인 — match 모듈과 별개)
│   └── Repositories/
│       ├── LineupRepositoryImpl.swift
│       └── AttendeeRepositoryImpl.swift          (두 endpoint async let 조인)
└── Presentation/
    ├── ViewModels/
    │   ├── LineupViewModel.swift                 (조회 + 추천 + 편집 진입)
    │   └── LineupEditViewModel.swift             (편집·검증·저장)
    └── Views/
        ├── LineupView.swift                       (메인)
        ├── LineupEditView.swift                   (편집 모달)
        ├── LineupDiamondView.swift                (다이아몬드)
        ├── BattingOrderListView.swift             (타순 1~9)
        └── LineupAssignSheet.swift                (포지션·타순 배정 시트)
```

### Tuist `Project.swift` 변경

```swift
let lineupFeature = frameworkTarget(
    name: "DugoutLineupFeature",
    sourcesPath: "Features/Lineup/Sources",
    dependencies: [
        .target(name: "DugoutCoreNetwork"),
        .target(name: "DugoutDesignSystem"),
    ]
)

// matchFeature dependencies 에 추가:
//     .target(name: "DugoutLineupFeature"),
// App 타겟 dependencies 에도 추가
// project.targets 배열에 lineupFeature 추가
```

> Match → Lineup 의존성을 명시적으로 허용 (Feature 간 의존 1건 추가). 모듈 다이어그램 갱신: docs/TDD.md.

### 모듈 경계 — 중복 코드 정당화

다음 타입들이 다른 모듈에도 존재하지만 Lineup 모듈에 자체 정의:

| 타입 | 다른 곳 정의 | Lineup 자체 정의 이유 |
|---|---|---|
| `BaseballPosition` | `DugoutAuthFeature.User.swift:63` | Feature 독립 원칙 (TeamRole 과 동일). rawValue + displayName 동일 |
| (Attendance summary 식별) | `DugoutMatchFeature` Attendance | Feature 간 의존 회피. 부분집합만 사용 |
| (TeamMember 등번호) | `DugoutMatchFeature.TeamMemberRef` | 동상 |

후속 정리 PR 에서 공유 모듈로 통합 가능 (TeamRole + BaseballPosition + 식별자 DTO 묶음).

---

## 4. 데이터 모델

### BaseballPosition (Auth 와 동일)

```swift
/// AuthFeature의 BaseballPosition과 동일한 정의 — Feature 독립 원칙에 따라 각자 소유.
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

    public static var fieldPositions: [BaseballPosition] {
        [.pitcher, .catcher, .firstBase, .secondBase, .thirdBase,
         .shortStop, .leftField, .centerField, .rightField]
    }
}
```

### Lineup (서버 상태)

```swift
public struct Lineup: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let matchId: Int64
    public let teamId: Int64
    public let isAiGenerated: Bool
    public let isConfirmed: Bool
    public let confirmedAt: Date?
    public let entries: [LineupEntry]
}

public struct LineupEntry: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let userId: Int64
    public let nickname: String
    public let position: BaseballPosition
    public let battingOrder: Int?    // nil = 벤치
    public let isBench: Bool
}
```

### LineupDraft (편집 중 상태)

```swift
public struct LineupDraft: Sendable, Equatable {
    public var entries: [LineupDraftEntry]
}

public struct LineupDraftEntry: Sendable, Equatable, Identifiable, Hashable {
    public let id: UUID
    public let userId: Int64
    public let nickname: String
    public let jerseyNumber: Int?
    public var position: BaseballPosition
    public var battingOrder: Int?
    public var isBench: Bool
}
```

### LineupRecommendation

```swift
public struct LineupRecommendation: Sendable, Equatable {
    public let matchId: Int64
    public let source: String        // "AI" | "STUB"
    public let isAiGenerated: Bool
    public let draft: LineupDraft    // 즉시 편집 가능 — recommendation entries + attendees 의 nickname/jersey 조인
}
```

### Attendee

```swift
public struct Attendee: Sendable, Equatable, Identifiable, Hashable {
    public let userId: Int64
    public let nickname: String
    public let jerseyNumber: Int?
    public var id: Int64 { userId }
}
```

### Repository 프로토콜

```swift
public protocol LineupRepository: Sendable {
    func fetchLineup(matchId: Int64) async throws -> Lineup

    /// AI 추천 호출. 백엔드는 matchId 만 받음 (출석자는 백엔드가 자체 조회).
    /// `attendees` 는 응답 entries 에 nickname/jerseyNumber 가 없으므로
    /// 클라이언트가 LineupDraft 로 변환할 때 조인용으로 전달 (Repository 가 내부 enrich).
    func recommend(matchId: Int64, attendees: [Attendee]) async throws -> LineupRecommendation

    func saveLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
    func updateLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
}

public protocol AttendeeRepository: Sendable {
    /// /api/v1/matches/{matchId}/attendance + /api/v1/teams/{teamId}/members 두 endpoint async let 조인.
    /// status in {ATTEND, LATE} 인 응답만 + 등번호 매칭.
    func fetchAttendees(matchId: Int64, teamId: Int64) async throws -> [Attendee]
}

public struct SaveLineupRequest: Sendable, Equatable {
    public let entries: [LineupDraftEntry]
}
```

### DTO 매핑

**LineupDTO** — 백엔드 `LineupResponse`:
```swift
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
    func toDomain() -> Lineup? { ... }
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
        return LineupEntry(id: id, userId: userId, nickname: nickname,
                           position: pos, battingOrder: battingOrder, isBench: isBench)
    }
}
```

**LineupRecommendationDTO** — entries 는 server-side id 없음 (`LineupEntryPayload`):
```swift
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

**LineupRequestDTO** — POST/PUT body:
```swift
struct LineupEntryPayloadDTOEncodable: Encodable, Sendable {
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
    let entries: [LineupEntryPayloadDTOEncodable]

    init(_ request: SaveLineupRequest) {
        self.entries = request.entries.map(LineupEntryPayloadDTOEncodable.init)
    }
}
```

---

## 5. 화면 컴포지션

### LineupView (메인)

상태별 분기:

**Empty 상태 (라인업 없음)**:
```
┌─────────────────────────────────────┐
│ ← 라인업                              │
├─────────────────────────────────────┤
│ [DGEmptyState]                       │
│   ⚾ 아직 라인업이 없어요              │
│   AI 추천으로 자동 배정하거나          │
│   직접 작성할 수 있어요                │
│                                       │
│ [DGButton .primary] AI 추천 받기      │   주장/매니저만
│ [DGButton .secondary] 직접 작성하기    │   주장/매니저만
└─────────────────────────────────────┘
```

일반 멤버 + empty: "주장이 라인업을 등록하면 여기서 확인할 수 있어요" DGEmptyState 만.

**Loaded 상태**:
```
┌─────────────────────────────────────┐
│ ← 라인업                              │
├─────────────────────────────────────┤
│ [DGBadge AI/수동] [DGBadge 확정/임시]│   상태 표시
├─────────────────────────────────────┤
│ [DGCard] LineupDiamondView           │   9 포지션 시각화, DH 별도
├─────────────────────────────────────┤
│ [DGCard] 타순                        │
│  1. 김** (SS) #7                    │
│  ...                                  │
│  9. 이** (P) #20                    │
├─────────────────────────────────────┤
│ [DGCard] 지명타자 (DH 존재 시)         │
│  최** (#30)                          │
├─────────────────────────────────────┤
│ [DGCard] 벤치 N명 (벤치 있을 때)       │
│  정** · 윤**                         │
├─────────────────────────────────────┤
│ [DGButton .primary] 편집              │   주장/매니저 + !isConfirmed
│ [DGButton .secondary] AI 다시 추천    │   주장/매니저 + !isConfirmed
└─────────────────────────────────────┘
```

`isConfirmed == true` 인 경우 편집·AI 다시 추천 버튼 미노출 (4-A 범위에선 확정 기능 자체가 없지만 백엔드가 confirm 한 라인업은 수정 차단).

### LineupDiamondView

`GeometryReader` 안에 9 포지션을 비율 좌표로 배치. 각 자리에 작은 chip (nickname 첫 2글자 + #등번호). 비어있으면 점선 윤곽.

| 포지션 | (x, y) 비율 |
|---|---|
| P  | (0.50, 0.70) |
| C  | (0.50, 0.92) |
| 1B | (0.78, 0.55) |
| 2B | (0.62, 0.40) |
| 3B | (0.22, 0.55) |
| SS | (0.38, 0.40) |
| LF | (0.20, 0.18) |
| CF | (0.50, 0.10) |
| RF | (0.80, 0.18) |

다이아몬드 백그라운드는 단순 그라데이션 + 단정한 외곽선. DH 는 다이아몬드 외부 (LineupView 에서 별도 카드).

### LineupEditView (fullScreenCover 또는 sheet)

```
┌─────────────────────────────────────┐
│ [취소]    라인업 편집     [저장]      │   toolbar
├─────────────────────────────────────┤
│ (안내 배너: AI 추천 결과로 채워졌어요   │   editSource == .recommendation + 기존 lineup 존재
│  저장 시 기존 라인업이 덮어쓰여집니다)  │
├─────────────────────────────────────┤
│ [DGCard] LineupDiamondView 미니        │   draft 미리보기 (작은 사이즈)
├─────────────────────────────────────┤
│ [DGSegmentedControl: 보기]            │
│  [ 출석자 ][ 타순 ]                  │
├─────────────────────────────────────┤
│ "출석자" 탭                            │
│ 출석자 N명                            │
│ ─────────────────────                │
│ 김** #7 · SS · 2번             [변경] │
│ 박** #15 · 미배정              [지정] │   미배정
│ 이** #20 · 벤치                [복귀] │   isBench == true
│ ...                                   │
├─────────────────────────────────────┤
│ "타순" 탭                             │
│ 타순 1~9 (필드 9 + DH 0~1)            │
│ ─────────────────────                │
│ 1. 김** (SS)                  ↑ ↓   │   위/아래 버튼 (1번은 ↑ disabled)
│ 2. 박** (CF)                  ↑ ↓   │
│ ...                                   │
├─────────────────────────────────────┤
│ [검증 에러 영역 — .danger 색]          │
│  · 1루수 자리가 비어있어요             │
│  · 5번 타순이 비어있어요               │
└─────────────────────────────────────┘
```

저장 버튼은 `canSubmit == false` 일 때 disabled.

### LineupAssignSheet (sheet)

"변경"/"지정" 탭 시 띄움. 단일 entry 의 position + battingOrder + isBench 결정.

```
┌─────────────────────────────────────┐
│           ▔▔▔                       │
│  김** 배정                  [X]      │
├─────────────────────────────────────┤
│ 포지션                                │
│ ┌────┬────┬────┬────┐               │
│ │ P  │ C  │ 1B │ 2B │               │   2행 2열 grid + DH/벤치 별도 행
│ ├────┼────┼────┼────┤               │   다른 사람이 차지한 자리는 dim + "○○님" 배지
│ │ 3B │ SS │ LF │ CF │               │
│ ├────┼────┼────┴────┤               │
│ │ RF │ DH │  벤치    │               │
│ └────┴────┴─────────┘               │
├─────────────────────────────────────┤
│ 타순 (포지션 선택 후 활성, 벤치는 비활성)│
│ [1][2][3][4][5][6][7][8][9]          │   chip — 이미 다른 사람이 차지한 번호 dim
├─────────────────────────────────────┤
│ [DGButton .primary] 확정              │   포지션 + (필드/DH면 타순) 선택해야 활성
└─────────────────────────────────────┘
```

"확정" 누르면 draft 의 해당 entry 업데이트. 다른 entry 와 충돌(같은 필드 포지션 차지) 발생 시 그 entry 는 자동으로 "미배정" 으로 reset (LineupEditViewModel 책임).

---

## 6. ViewModel 상태 머신

### LineupViewModel

```swift
@MainActor @Observable
public final class LineupViewModel {
    public enum State: Sendable {
        case idle
        case loading                  // GET /lineup
        case empty                    // 404 / LINEUP_NOT_FOUND
        case loaded(Lineup)
        case recommending             // POST /recommend
        case failed(String)
    }
    public enum EditSource: Sendable, Equatable {
        case empty(attendees: [Attendee])                       // 직접 작성
        case recommendation(LineupRecommendation, attendees: [Attendee])  // AI 추천 결과
        case existing(Lineup, attendees: [Attendee])            // 기존 라인업 수정
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

    public func load() async { ... }                // GET /lineup → loaded | empty
    public func tapRecommend() async { ... }        // attendees 먼저 fetch → recommend → editSource = .recommendation + presentEdit = true
    public func tapEditExisting() async { ... }     // attendees fetch → editSource = .existing + presentEdit = true
    public func tapWriteFromScratch() async { ... } // attendees fetch → editSource = .empty + presentEdit = true
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

### LineupEditViewModel

```swift
@MainActor @Observable
public final class LineupEditViewModel {
    public enum State: Sendable, Equatable {
        case editing
        case submitting
        case success(Lineup)
        case failed(String)
    }
    public enum SelectedTab: Sendable, Equatable {
        case roster
        case battingOrder
    }

    public private(set) var state: State = .editing
    public var draft: LineupDraft
    public var attendees: [Attendee]    // 변경 불가, 출석자 원본
    public var selectedTab: SelectedTab = .roster
    public var presentAssignSheet: Bool = false
    public var assignTargetEntry: LineupDraftEntry? = nil

    public let matchId: Int64
    public let isUpdate: Bool            // 기존 라인업 수정 (PUT vs POST)
    public let showOverwriteBanner: Bool // editSource == .recommendation && 기존 라인업 존재
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
            self.draft = LineupDraft(entries: attendees.map { ... unassigned entry ... })
            self.isUpdate = existingLineupExists
            self.showOverwriteBanner = false
        case .recommendation(let rec, let attendees):
            self.attendees = attendees
            self.draft = rec.draft
            self.isUpdate = existingLineupExists
            self.showOverwriteBanner = existingLineupExists
        case .existing(let lineup, let attendees):
            self.attendees = attendees
            self.draft = LineupDraft(entries: lineup.entries.map { ... toDraft ... })
            self.isUpdate = true
            self.showOverwriteBanner = false
        }
    }

    public func openAssignSheet(for entry: LineupDraftEntry) {
        assignTargetEntry = entry
        presentAssignSheet = true
    }

    public func assign(position: BaseballPosition, battingOrder: Int?, isBench: Bool) { ... }
    public func sendToBench(_ entry: LineupDraftEntry) { ... }
    public func swapBattingOrder(from: Int, to: Int) { ... }

    public var validationErrors: [String] {
        // 누락 / 중복 검증 — 위 §5 의 알고리즘
    }
    public var canSubmit: Bool {
        if case .submitting = state { return false }
        return validationErrors.isEmpty
    }

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

---

## 7. 진입점 (MATCH-3 변경)

`MatchDetailView` 의 `attendanceSummaryCard` 다음, `summaryButton` 위에 라인업 CTA 카드 추가:

```swift
// MatchDetailView 의 content/.loaded 영역
ScrollView {
    VStack(spacing: DGSpacing.lg) {
        matchInfoCard(detail.match)
        myVoteCard(detail.match)
        attendanceSummaryCard(detail.attendance)
        lineupCard(detail.match)        // ← 신규
        if viewModel.isManager {
            summaryButton
        }
    }
    ...
}
```

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

이 카드는 라인업 존재 여부 표시 없이 단순화 — `LineupView` 가 진입 후 자체 fetch.

`MatchDetailView` 가 `import DugoutLineupFeature` 추가 필요.

---

## 8. 에러 / 엣지 케이스

| 시나리오 | 동작 |
|---|---|
| `GET /lineup` 404 / `LINEUP_NOT_FOUND` | `state = .empty`. Empty UI 노출 |
| `GET /lineup` 기타 실패 | `state = .failed`. DGErrorState + 재시도 |
| `GET /attendees` (조인 호출) 실패 | toast 에러 메시지. `state` 변경 없음 |
| `POST /recommend` `INSUFFICIENT_ATTENDEES` (출석자 < 9) | toast "출석자가 9명 미만이에요" (warning) |
| `POST /recommend` `AI_SERVICE_UNAVAILABLE` | toast "AI 서비스에 일시적으로 접근할 수 없어요" (danger) |
| `POST /lineup` `LINEUP_ALREADY_EXISTS` (race) | 자동 PUT 재시도 (투명) |
| `PUT /lineup` `LINEUP_ALREADY_CONFIRMED` | inline 에러 "확정된 라인업은 수정할 수 없어요" |
| `PUT /lineup` `LINEUP_NOT_FOUND` (race) | inline 에러 "라인업을 찾을 수 없어요. 다시 시도해주세요" |
| `INVALID_LINEUP_POSITION` / `INVALID_INPUT` | inline 에러 — 클라이언트가 validate 했어야 함 (방어망) |
| 출석자 0명 직접 작성 | 빈 draft → validation errors 가득 → 저장 disabled |

### PII 가드

- `nickname`, `jerseyNumber` 평문 — UI 노출 OK
- 로그 (`print`, `os_log`, `NSLog`) 절대 금지
- M7 검증: `grep -rEn "(print|os_log|NSLog).*(nickname|jersey|attendee|member)" dugout-ios/Features/Lineup/Sources/` → 0건

---

## 9. Milestone 구성

| M | 범위 | 산출물 |
|---|---|---|
| **M1** | 브랜치 + Tuist 타겟 신설 + 빈 모듈 빌드 통과 | `Project.swift` 수정, `Features/Lineup/Sources/Placeholder.swift`, App + Match 의존성 추가 |
| **M2** | Domain 레이어 | 7개 신규 파일 — BaseballPosition, Lineup, LineupEntry, LineupDraft, LineupDraftEntry, LineupRecommendation, Attendee, LineupRepository, AttendeeRepository, SaveLineupRequest |
| **M3** | Data 레이어 | DTO 5종 + RepositoryImpl 2종 (LineupRepositoryImpl, AttendeeRepositoryImpl) |
| **M4** | `LineupView` + `LineupViewModel` + `LineupDiamondView` + `BattingOrderListView` (조회 화면, 편집은 stub) | 4 파일 |
| **M5** | `LineupEditView` + `LineupEditViewModel` + `LineupAssignSheet` (편집 + 검증 + 저장) | 3 파일 |
| **M6** | MATCH-3 `MatchDetailView.lineupCard` 추가 | `MatchDetailView.swift` 수정 |
| **M7** | 빌드 + PII + 백엔드 baseline + docs/TDD.md 갱신 + 통합 커밋 + main 머지 | (controller 머지) |

### 수동 검증 시나리오 (M7)

1. 주장 로그인 → 일정 탭 → 경기 카드 탭 → MatchDetailView 진입
2. 라인업 카드 탭 → LineupView empty 상태 ("아직 라인업이 없어요")
3. "AI 추천 받기" 탭 → recommending 로딩 → LineupEditView 자동 진입
4. 다이아몬드 미리보기에 9명 배치 확인
5. 출석자 탭 → 한 명의 "변경" → AssignSheet → 포지션 변경 → 확정 → draft 갱신
6. 타순 탭 → 1번과 3번 swap (↑↓) → draft 갱신
7. "저장" 탭 → POST → 성공 → 편집 dismiss → LineupView `.loaded`
8. "편집" 탭 → 기존 라인업이 draft 로 변환 → 한 명 벤치 → 저장 (PUT) → 성공
9. "AI 다시 추천" → 안내 배너 표시 → 저장 → 기존 덮어쓰기
10. 일반 멤버 → readonly (편집/추천 CTA 미노출)
11. (코드 리뷰) 출석자 9명 미만 경기 → "AI 추천" 시 toast
12. (코드 리뷰) `validationErrors` 분기 정확성

### Git 전략

```bash
git checkout -b feature/phase4-lineup-a
# M1 ~ M7
git checkout main
git merge --no-ff feature/phase4-lineup-a
git branch -d feature/phase4-lineup-a
git push origin main
```

---

## 10. PR 완료 체크리스트

- [ ] `xcodebuild -quiet build` 성공, warnings 0
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공
- [ ] 수동 시나리오 1~10 통과, 11~12 코드 리뷰
- [ ] Swift 6 Sendable 위반 0
- [ ] PII 로그 노출 0
- [ ] 새 ErrorCode 추가 없음
- [ ] 도메인 용어 준수
- [ ] `docs/TDD.md` 갱신: DugoutLineupFeature 모듈 추가 + Match→Lineup 의존성 + Phase 4-A 책임 단락
- [ ] feature/phase4-lineup-a 의 7 commit + main 머지 + push 완료

---

## 11. 추후 Phase

- **Phase 4-B**: 확정 + 라인업 카드 이미지 공유 (백엔드는 stub 만)
- **드래그&드롭 polish**: SwiftUI Drag/Drop API
- **AI 모드 전환**: BALANCED vs COMPETITIVE 팀 설정 화면
- **좌우 타석 균형 시각화**: 헝가리안 결과의 fairness
- **알림 트리거**: 라인업 확정 시 멤버에게 푸시 (3-C.2 후속과 합쳐서)
- **공유 모듈 추출**: TeamRole + BaseballPosition + 식별자 DTO 묶음 별도 모듈로
