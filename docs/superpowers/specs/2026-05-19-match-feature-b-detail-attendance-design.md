# Phase 3 MATCH-B: 경기 상세 + 출석 응답 Design

**작성일**: 2026-05-19
**상태**: 설계 합의 완료, implementation plan 대기
**선행**: Phase 3 MATCH-A (일정·등록) — main 머지 완료

---

## 1. 목표

iOS `DugoutMatchFeature` 모듈에 **MATCH-3 경기 상세**와 **MATCH-4 출석 응답** 두 화면을 추가하여 사용자가 등록된 경기의 정보를 조회하고 출석 응답을 등록/변경할 수 있게 한다. 백엔드 Attendance 도메인(POST/PUT/GET `/api/v1/matches/{matchId}/attendance`)은 이미 구현되어 있으므로 **iOS 단독 작업**.

---

## 2. 스코프

### 포함
- **MATCH-3 경기 상세 화면**: 경기 정보 헤더 + 내 응답 카드(+ CTA) + 출석 현황 카드(컬럼 통계 + 응답자 리스트) + 주장 전용 "전체 보기" 버튼(토스트 안내)
- **MATCH-4 출석 응답 시트**: 메인 3개(참가/불참/미정) + 부분 참여 토글(늦참/조퇴) + 사유 입력 + 저장
- **MatchListView 셀 탭 네비게이션**: `NavigationStack` 도입, 상세 화면 push
- **신규 응답 / 응답 변경 분기**: 백엔드 POST(최초) vs PUT(변경) 자동 선택 + `ALREADY_VOTED` race 대응

### 제외
- AI 예측 카드 (dugout-ai 연동 — Phase 후속)
- MATCH-5 출석 요약 본 화면 (Phase 3-C)
- 경기 편집/삭제 메뉴 (백엔드는 있으나 화면 별도, Phase 후속)
- 카카오 공유 (Phase 3-C)
- Unit Test 타겟 (Phase 2 이후)

### 백엔드 변경
**없음**. 다음 기존 엔드포인트만 사용:
- `GET /api/v1/matches/{matchId}` — 경기 상세
- `GET /api/v1/matches/{matchId}/attendance` — 출석 요약 + 응답 리스트
- `POST /api/v1/matches/{matchId}/attendance` — 최초 응답 (201)
- `PUT /api/v1/matches/{matchId}/attendance` — 응답 변경 (200)

---

## 3. 모듈 구조 & 파일 배치

`DugoutMatchFeature` 내부에 `Attendance/` 서브 폴더 추가. 같은 Tuist 타겟 안에 두되 폴더로 격리한다.

```
dugout-ios/Features/Match/Sources/
├── Domain/
│   ├── Entities/
│   │   ├── Match.swift                       (기존)
│   │   ├── MatchStatus.swift                 (기존)
│   │   ├── AttendanceStatus.swift            (신규)
│   │   ├── AttendanceVote.swift              (신규)
│   │   └── AttendanceSummary.swift           (신규)
│   └── Repositories/
│       ├── MatchRepository.swift             (수정 — fetchDetail 추가)
│       └── AttendanceRepository.swift        (신규)
├── Data/
│   ├── DTOs/
│   │   ├── MatchDTO.swift                    (기존)
│   │   ├── MatchRequestDTO.swift             (기존)
│   │   ├── AttendanceDTO.swift               (신규)
│   │   └── AttendanceRequestDTO.swift        (신규)
│   └── Repositories/
│       ├── MatchRepositoryImpl.swift         (수정 — fetchDetail 추가)
│       └── AttendanceRepositoryImpl.swift    (신규)
└── Presentation/
    ├── ViewModels/
    │   ├── MatchListViewModel.swift          (수정 — currentUserId 전파)
    │   ├── CreateMatchViewModel.swift        (기존)
    │   ├── MatchDetailViewModel.swift        (신규)
    │   └── AttendanceVoteViewModel.swift     (신규)
    └── Views/
        ├── MatchListView.swift               (수정 — NavigationStack + Link + currentUserId)
        ├── MatchCalendarGrid.swift           (기존)
        ├── CreateMatchView.swift             (기존)
        ├── MatchDetailView.swift             (신규)
        ├── AttendanceVoteSheet.swift         (신규)
        └── AttendanceStatusButton.swift      (신규)
```

**App 레이어 수정 (Feature 외부)**:
- `dugout-ios/App/Sources/MainTabView.swift` — `.schedule` 케이스 `ScheduleTabHost(authViewModel:)` 주입
- `dugout-ios/App/Sources/ScheduleTabHost.swift` — `authViewModel` 의존성, `NavigationStack` 도입, `currentUserId` 추출 후 `MatchListView` 에 전달

`Project.swift` 변경 없음 (같은 타겟 내 파일 추가만, App 타겟의 Feature 의존성은 이미 등록됨).

### 책임 경계
- `MatchDetailViewModel`: Match 상세 + Attendance 요약 동시 fetch. 두 Repository 의존.
- `AttendanceVoteViewModel`: 시트 생명주기에만 묶임. dismiss 시 부모 콜백으로 갱신 트리거.
- `AttendanceVoteSheet`는 `matchId` + `isUpdate` + `existingVote?` + `onCompleted` 콜백만 받음. `currentUserId` 불필요.

---

## 4. 데이터 모델

### 도메인 엔티티

```swift
public enum AttendanceStatus: String, Sendable, Codable, CaseIterable {
    case attend     = "ATTEND"
    case absent     = "ABSENT"
    case maybe      = "MAYBE"
    case late       = "LATE"
    case earlyLeave = "EARLY_LEAVE"

    public var koreanLabel: String { ... }   // "참가" / "불참" / "미정" / "늦참" / "조퇴"
    public var emoji: String { ... }         // "✅" / "❌" / "❓" / "⏰" / "🚪"
}

public struct AttendanceVote: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let matchId: Int64
    public let userId: Int64
    public let nickname: String
    public let status: AttendanceStatus
    public let reason: String?
    public let respondedAt: Date
}

public struct AttendanceSummary: Sendable, Equatable {
    public let matchId: Int64
    public let totalMembers: Int
    public let respondedCount: Int
    public let pendingCount: Int
    public let statusCounts: [AttendanceStatus: Int]
    public let votes: [AttendanceVote]

    public func myVote(userId: Int64) -> AttendanceVote? {
        votes.first { $0.userId == userId }
    }
}
```

### Repository 프로토콜

```swift
public protocol MatchRepository: Sendable {
    func fetchMatches(teamId: Int64, from: Date?, to: Date?) async throws -> [Match]
    func createMatch(teamId: Int64, request: CreateMatchRequest) async throws -> Match
    func fetchDetail(matchId: Int64) async throws -> Match           // 신규
}

public protocol AttendanceRepository: Sendable {
    func fetchSummary(matchId: Int64) async throws -> AttendanceSummary
    func createVote(matchId: Int64, request: AttendanceVoteRequest) async throws -> AttendanceVote
    func updateVote(matchId: Int64, request: AttendanceVoteRequest) async throws -> AttendanceVote
}

public struct AttendanceVoteRequest: Sendable {
    public let status: AttendanceStatus
    public let reason: String?
}
```

### DTO ↔ Domain 매핑

백엔드 응답은 Jackson SNAKE_CASE 글로벌 설정이므로 모든 DTO 가 `CodingKeys` 로 명시 매핑한다 (기존 `MatchDTO` 와 동일 패턴).

```swift
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
        case matchId = "match_id"
        case userId = "user_id"
        case respondedAt = "responded_at"
    }

    func toDomain() -> AttendanceVote? { ... }   // status 파싱 실패 시 nil
}

struct AttendanceSummaryDTO: Decodable, Sendable {
    let matchId: Int64
    let totalMembers: Int
    let respondedCount: Int
    let pendingCount: Int
    let statusCounts: [String: Int]
    let votes: [AttendanceVoteDTO]

    enum CodingKeys: String, CodingKey {
        case matchId = "match_id"
        case totalMembers = "total_members"
        case respondedCount = "responded_count"
        case pendingCount = "pending_count"
        case statusCounts = "status_counts"
        case votes
    }

    func toDomain() -> AttendanceSummary { ... }
}

struct AttendanceVoteRequestDTO: Encodable, Sendable {
    let status: String
    let reason: String?
}
```

---

## 5. 화면 컴포지션

### MATCH-3 · MatchDetailView

`ScrollView` 안에 4개 카드 + 주장 전용 버튼.

```
┌─────────────────────────────────────┐
│ ← 경기 상세                          │  navigationTitle(.inline)
├─────────────────────────────────────┤
│ [DGCard] 경기 정보 헤더              │
│   [D-3] [예정] (DGBadge x 2)        │
│   5월 23일 (토) · 오후 8:00          │
│   집합: 오후 7:30                   │
│   vs 베어스FC                       │
│   📍 잠실야구장                      │
│   ⏱ 투표 마감: 5월 22일 22:00       │  voteDeadline 있을 때만
│   📝 메모 …                        │  memo 있을 때만
├─────────────────────────────────────┤
│ [DGCard] 내 응답                    │
│   상태: [참가 ✅] (또는 "아직 응답 안 함")│
│   사유: …                          │  reason 있을 때만
│   응답 시각: 5/19 14:30              │  respondedAt 있을 때만
│   [응답 변경] / [지금 응답하기] CTA   │  canVote 면 활성
│   ↳ 비활성 시 안내: "취소된 경기"     │
│      / "투표 마감 시간이 지났어요"    │
├─────────────────────────────────────┤
│ [DGCard] 출석 현황                  │
│   ┌──참가──┬──불참──┬──미응답──┐    │
│   │   6   │   1   │    7    │    │
│   └────────┴────────┴─────────┘    │  count == 0 면 dim
│   보조: 미정 1 · 늦참 0 · 조퇴 0    │
│   ───── 응답자 ─────                │
│   👤 홍** · 참가 ✅ · 14:30         │
│   👤 김** · 불참 ❌ · 회식 · 13:20  │
├─────────────────────────────────────┤
│ [전체 보기] (주장만)                 │  탭 → 토스트
└─────────────────────────────────────┘
```

**state**:
```swift
@MainActor @Observable
public final class MatchDetailViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded(detail: MatchDetail)
        case failed(String)
    }
    public struct MatchDetail: Sendable, Equatable {
        public let match: Match
        public let attendance: AttendanceSummary
    }

    public private(set) var state: State = .idle
    public var presentVoteSheet: Bool = false
    public var toast: DGToastItem? = nil    // 주장 "전체 보기" 등 일회성 안내. DGToast 가 3초 후 자동 nil 처리

    public let matchId: Int64
    public let currentUserId: Int64
    public let isManager: Bool

    private let matchRepository: any MatchRepository
    private let attendanceRepository: any AttendanceRepository

    public func load() async { ... }
    public func tapVote() { presentVoteSheet = true }
    public func tapSummary() {
        toast = DGToastItem(message: "출석 요약은 다음 업데이트에 제공돼요", kind: .info)
    }
    public func onVoteCompleted(_ vote: AttendanceVote) async {
        presentVoteSheet = false
        await load()   // 풀 리프레시
    }

    public var canVote: Bool {
        guard case .loaded(let detail) = state else { return false }
        if detail.match.status == .cancelled { return false }
        if let deadline = detail.match.voteDeadline, deadline < Date() { return false }
        return true
    }
    public var voteBlockedReason: String? {
        guard case .loaded(let detail) = state else { return nil }
        if detail.match.status == .cancelled { return "취소된 경기예요" }
        if let deadline = detail.match.voteDeadline, deadline < Date() {
            return "투표 마감 시간이 지났어요"
        }
        return nil
    }
    public var myVote: AttendanceVote? {
        guard case .loaded(let detail) = state else { return nil }
        return detail.attendance.myVote(userId: currentUserId)
    }
}
```

### MATCH-4 · AttendanceVoteSheet (`.sheet` slide-up)

```
┌─────────────────────────────────────┐
│           ▔▔▔                       │  drag indicator (SwiftUI 기본)
│  출석 응답                  [X]      │  toolbar cancel
├─────────────────────────────────────┤
│  ⚾ 5월 23일 (토) vs 베어스FC       │  read-only 컨텍스트
├─────────────────────────────────────┤
│  메인 응답                          │
│  ┌────────┬────────┬────────┐     │
│  │ 참가 ✅ │ 불참 ❌ │ 미정 ❓ │     │  AttendanceStatusButton x 3
│  └────────┴────────┴────────┘     │  isSelected: spring overshoot
├─────────────────────────────────────┤
│  부분 참여 (메인=참가일 때만)        │
│  ◯ 늦참 ⏰   ◯ 조퇴 🚪             │  라디오 — 둘 다 해제 가능
├─────────────────────────────────────┤
│  사유 (선택)                        │
│  [DGTextField "예: 회식, 부상 등"]  │  200자 제한 (백엔드 Size(200))
├─────────────────────────────────────┤
│  [응답 저장]                        │  DGButton(.primary)
│  ↳ 에러 시 inline 메시지 (.danger)  │
└─────────────────────────────────────┘
```

**ViewModel**:
```swift
@MainActor @Observable
public final class AttendanceVoteViewModel {
    public enum State: Sendable, Equatable {
        case editing
        case submitting
        case success(AttendanceVote)
        case failed(String)
    }
    public enum MainChoice: Sendable { case attend, absent, maybe }
    public enum PartialChoice: Sendable { case none, late, earlyLeave }

    public private(set) var state: State = .editing
    public var mainChoice: MainChoice = .attend
    public var partialChoice: PartialChoice = .none
    public var reason: String = ""

    public let matchId: Int64
    private let existingVote: AttendanceVote?    // nil 이면 신규
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
            (mainChoice, partialChoice) = Self.decompose(existingVote.status)
            reason = existingVote.reason ?? ""
        }
    }

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

### Status 매핑 룰 (UI ↔ 도메인)

| UI 조합 | resolvedStatus |
|---|---|
| 메인=참가, 부분=해제 | `ATTEND` |
| 메인=참가, 부분=늦참 | `LATE` |
| 메인=참가, 부분=조퇴 | `EARLY_LEAVE` |
| 메인=불참 (부분 자동 해제·비활성) | `ABSENT` |
| 메인=미정 (부분 자동 해제·비활성) | `MAYBE` |

메인을 변경할 때 부분 토글은 자동으로 `.none` 으로 리셋 (메인=참가 외에는 disabled).

---

## 6. 네비게이션 흐름

### NavigationStack 도입

현재 `MainTabView` 의 `.schedule` 케이스는 별도 컴포넌트 `ScheduleTabHost` 를 호출하고, 이 호스트가 `HomeRepository.fetchMyTeams()` 로 팀 목록을 가져와 `MatchListView` 를 인스턴스화한다. `AuthViewModel` 의존성은 현재 없음.

**수정 방식**:
1. `MainTabView` 에서 `ScheduleTabHost(authViewModel: authViewModel)` 로 의존성 주입
2. `ScheduleTabHost` 가 `authViewModel.currentUser?.id` 를 추출
3. `ScheduleTabHost` 가 `NavigationStack` 으로 `MatchListView` 를 감쌈

```swift
// MainTabView.swift (수정)
case .schedule:
    ScheduleTabHost(authViewModel: authViewModel)

// ScheduleTabHost.swift (수정)
struct ScheduleTabHost: View {
    @Bindable var authViewModel: AuthViewModel
    @State private var teams: [MyTeam]?
    @State private var errorMessage: String?
    private let repository: any HomeRepository = HomeRepositoryImpl()

    var body: some View {
        Group {
            if let errorMessage { ... }
            else if let teams, let firstTeam = teams.first,
                    let currentUserId = authViewModel.currentUser?.id {
                NavigationStack {
                    MatchListView(
                        teamId: firstTeam.teamId,
                        isManager: firstTeam.role == .captain || firstTeam.role == .manager,
                        currentUserId: currentUserId
                    )
                }
            } else if teams != nil {
                // 팀 없음 또는 로그인 끊김 → 기존 empty/팀 안내
                DGEmptyState(...)
            } else {
                DGLoadingState(...)
            }
        }
        .task { await load() }
    }
}
```

`currentUserId == nil` (로그아웃 race) 인 경우 empty state 로 fallback. 진입 자체 차단.

### 화면 전이

```
MatchListView                  (NavigationStack root)
   │
   │ NavigationLink(value: match.id)
   │ .navigationDestination(for: Int64.self) { matchId in
   │     MatchDetailView(matchId: matchId,
   │                     currentUserId: currentUserId,
   │                     isManager: isManager)
   │ }
   ▼
MatchDetailView                (push)
   │
   │ viewModel.presentVoteSheet = true
   ▼
AttendanceVoteSheet            (.sheet slide-up)
   │
   │ onCompleted(vote) → MatchDetailViewModel.onVoteCompleted(vote)
   │                  → presentVoteSheet = false
   │                  → load()  (풀 리프레시)
   ▼
MatchDetailView                (refresh)
```

### 데이터 갱신 정책

| 변경원 | 영향 받는 화면 | 갱신 방법 |
|---|---|---|
| AttendanceVoteSheet → MatchDetailView | MatchDetailView | 콜백 후 `await load()` 풀 리프레시 |
| MatchDetailView → MatchListView | MatchListView | **갱신 불필요** (리스트 카드에 출석 카운트 미표시) |

옵션 비교: 로컬 머지(RTT 0) 검토했으나 `statusCounts` 재계산·본인 응답 교체·nickname 보존의 버그 여지가 커서 풀 리프레시 채택. 출석 응답은 빈번하지 않으므로 RTT 1 비용 감수.

### `currentUserId` 전파

```
AuthViewModel.currentUser?.id (Int64?, User.id 는 Int64)
  → MainTabView (.schedule 케이스)
  → ScheduleTabHost(authViewModel:)             ← 신규 의존성
  → MatchListView(currentUserId:)               ← 신규 파라미터
  → MatchDetailView(currentUserId:)             ← 신규 화면
  // AttendanceVoteSheet 는 matchId + existingVote 만 알면 됨
```

`currentUserId == nil` 인 경우 `ScheduleTabHost` 가 empty state 를 반환하여 진입 자체가 차단됨.

---

## 7. 에러 & 엣지 케이스

### POST → PUT 자동 재시도 (`ALREADY_VOTED` race)

신규 응답이라 판단해 POST 를 보냈는데 백엔드가 `ALREADY_VOTED (409)` 를 반환한 경우(멀티 디바이스, stale 상태) → 같은 페이로드로 PUT 재시도. 사용자에겐 투명.

`APIError.server(let response, _)` 의 `response.code == "ALREADY_VOTED"` 로 식별. `APIError` 의 기존 구조로 가능, 추가 노출 작업 불필요.

### 마감 시간 / 취소 사전 가드

`MatchDetailViewModel.canVote` 가 다음 조건 모두 만족할 때만 true:
- `match.status != .cancelled`
- `match.voteDeadline == nil` 또는 `voteDeadline > now`

조건 불충족 시 CTA 비활성 + `voteBlockedReason` 안내. 서버측 검증도 동시에 존재하므로 race 발생 시 백엔드 메시지로 안내(`VOTE_DEADLINE_PASSED` / `MATCH_ALREADY_CANCELLED`).

### 에러 메시지 매핑

`APIError.userMessage` 는 `.server` 케이스에서 백엔드 `response.message` (이미 한국어) 를 그대로 노출. 별도 매핑 테이블 불필요.

| 백엔드 ErrorCode | 사용자 메시지 |
|---|---|
| `VOTE_DEADLINE_PASSED` | "투표 마감 시간이 지났어요" |
| `MATCH_ALREADY_CANCELLED` | "취소된 경기예요" |
| `NOT_TEAM_MEMBER` | "팀 멤버만 응답할 수 있어요" |
| `VOTE_NOT_FOUND` (PUT 시) | "응답을 찾을 수 없어요" |
| `INVALID_ATTENDANCE_STATUS` | "잘못된 응답 상태예요" (실질 발생 불가, 방어) |
| `MATCH_NOT_FOUND` | "경기를 찾을 수 없어요" |
| `ALREADY_VOTED` | (사용자에게 노출 X — 자동 PUT 재시도) |

### 에러 상황별 UI 동작

| 상황 | UI |
|---|---|
| MatchDetail load 실패 | DGErrorState 전체 화면 + 재시도 버튼 |
| Vote submit 실패 (4xx/네트워크) | sheet 유지, inline `.danger` 메시지, 재시도 가능 |
| Vote submit 성공 후 detail load 실패 | sheet 는 이미 dismiss. MatchDetailView 가 `.failed` 전환 (백엔드엔 정상 저장됨) |

### PII 가드

- `nickname`, `reason` 은 평문 — UI 노출은 OK
- `print`, `os_log`, crash report 에 **절대 금지**
- M6 검증 체크리스트에 `grep -r "print.*\(nickname\|reason\)" Features/Match` 0건 확인 포함

---

## 8. Milestone 구성

각 milestone 은 subagent 1개 dispatch. 완료 기준 = `xcodebuild -quiet build` 성공 (warning 0) + conventional commit.

| M | 범위 | 산출물 |
|---|---|---|
| **M1** | Attendance Domain 레이어 | `AttendanceStatus`, `AttendanceVote`, `AttendanceSummary`, `AttendanceRepository`, `AttendanceVoteRequest` |
| **M2** | Attendance Data 레이어 + `MatchRepository.fetchDetail` | `AttendanceDTO`, `AttendanceRequestDTO`, `AttendanceRepositoryImpl`, `MatchRepositoryImpl.fetchDetail` |
| **M3** | MATCH-3 화면 골격 | `MatchDetailViewModel`, `MatchDetailView` (응답 카드 CTA 는 stub — sheet 미연결) |
| **M4** | MATCH-4 시트 | `AttendanceVoteViewModel`, `AttendanceVoteSheet`, `AttendanceStatusButton` |
| **M5** | 상세 ↔ 응답 연결 + 리스트 네비게이션 | M3↔M4 sheet 연결, `MatchListView` NavigationStack + Link, `currentUserId` 전파 |
| **M6** | 통합 + 문서 + 머지 | `MainTabView` + `ScheduleTabHost` 수정 (authViewModel 주입, currentUserId 전파), 수동 시나리오, `docs/TDD.md` Attendance 행 추가, main 머지 |

### 수동 검증 시나리오 (M6)

1. 로그인 → 일정 탭 → 경기 카드 탭 → MatchDetailView 진입
2. 정보 헤더(D-Day, 날짜, 상대팀, 구장, 메모) 정상 노출
3. 응답 안 한 상태 → "지금 응답하기" CTA → sheet 진입
4. 메인 "참가" + 부분 해제 → 저장 → sheet dismiss → 본인 응답 "참가 ✅", ATTEND 카운트 +1
5. "응답 변경" → "참가" + "늦참" + 사유 입력 → 저장 → 본인 응답 "늦참 ⏰", 사유 표시
6. "응답 변경" → "불참" → 부분 토글 자동 비활성 → 저장 → 본인 응답 "불참 ❌"
7. 주장 계정 진입 → "전체 보기" 노출, 탭 시 토스트 "출석 요약은 다음 업데이트에 제공돼요"
8. 일반 멤버 계정 → "전체 보기" 비표시
9. (코드 리뷰) `voteDeadline` 과거 경기 진입 시 CTA 비활성 + "투표 마감 시간이 지났어요"
10. (코드 리뷰) `status == CANCELLED` 경기 진입 시 CTA 비활성 + "취소된 경기예요"

### Git 전략

```bash
git checkout -b feature/phase3-match-b
# M1 ~ M6 진행 (각각 별도 커밋)
git checkout main
git merge --no-ff feature/phase3-match-b
# 브랜치 정리는 사용자 권한으로
```

---

## 9. PR 완료 체크리스트

- [ ] `xcodebuild -quiet build` 성공, warning 0
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공 (백엔드 손대지 않았어도 점검)
- [ ] 수동 시나리오 1~8 통과 (9~10 코드 리뷰)
- [ ] Swift 6 Sendable 위반 0 — 모든 새 타입 `Sendable`, ViewModel `@MainActor @Observable final class`
- [ ] `docs/TDD.md` Attendance 행 추가
- [ ] PII 로그 노출 0 — `grep -r "print.*\(nickname\|reason\)" dugout-ios/Features/Match` 빈 결과
- [ ] 도메인 용어 준수 — Attendance, Vote, Match (자유 번역·로마자 변형 없음)
- [ ] 새 ErrorCode 추가 없음 (백엔드 기존 enum 재사용)
- [ ] `MainTabView` 의 `currentUserId` 주입 경로가 `nil` 일 때 empty state 로 안전 대응

---

## 10. 추후 Phase

- **Phase 3-C (MATCH-5)**: 출석 요약 본 화면 (주장 전용) + 카카오 공유
- **별도 보강**: AI 예측 카드(dugout-ai 연동), 경기 편집/삭제 화면, 알림 트리거(투표 마감 임박, 결과 발표)
