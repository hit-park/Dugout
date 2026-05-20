# Phase 3 MATCH-C: 출석 요약 (주장 전용) Design

**작성일**: 2026-05-20
**상태**: 설계 합의 완료, implementation plan 대기
**선행**: Phase 3 MATCH-A (일정·등록), MATCH-B (상세·응답) — main 머지 완료

---

## 1. 목표

iOS `DugoutMatchFeature` 모듈에 **MATCH-5 출석 요약 화면**을 추가하여 주장 권한 사용자가 경기별 응답 통계 + 응답자·미응답자 리스트를 한눈에 확인할 수 있게 한다.

> **푸시 알림은 본 PR 범위 외**. 디자인 가이드의 "미응답자에게 알림 발송" 동작은 백엔드 FCM 인프라가 부재하여 후속 Phase (3-C.1 + 3-C.2)로 분리. 본 PR 에선 알림 버튼 UI 만 노출하고 탭 시 `DGToastItem` 안내("알림 기능은 준비 중이에요").

---

## 2. 스코프

### 포함
- 주장 전용 **MatchAttendanceSummaryView**: 통계 카드 + 필터 segment + 응답자 리스트 + 미응답자 리스트
- 필터 4종 (전체 / 참가 / 불참 / 미응답). "참가" 는 attend + late + earlyLeave 모두 묶음. "전체" 일 때만 미정 응답자가 별도 보조 섹션으로 노출
- 미응답자 = `(active TeamMembers).userId − summary.votes.userId`. iOS 단독 계산, 백엔드 변경 없음
- MATCH-3 "전체 보기" 버튼 → 토스트 제거, `NavigationLink { MatchAttendanceSummaryView(...) }` push
- `AttendanceVoteRow` 공통 컴포넌트 추출 (MATCH-3 와 MATCH-5 모두 사용)

### 제외
- 실제 FCM 푸시 발송 (3-C.1 + 3-C.2 별도 PR)
- 24h 알림 쿨다운 로직 (3-C.2)
- "stagger 40ms fade" 등 마이크로 애니메이션 (후속 polish)
- 멤버 행의 포지션 표시 (`TeamMemberResponse.positions` 노출 — 후속, 본 PR 은 jerseyNumber + role 만)
- pull-to-refresh
- 카카오 공유 (별도 PR)

### 백엔드 변경
**없음**. 다음 기존 endpoint 만 사용:
- `GET /api/v1/teams/{teamId}/members` → `List<TeamMemberResponse>`
- `GET /api/v1/matches/{matchId}/attendance` → `AttendanceSummaryResponse`

---

## 3. 모듈 구조 & 파일 배치

`DugoutMatchFeature` 내부에 새 도메인 (Team Member 조회) 자체 노출.

```
dugout-ios/Features/Match/Sources/
├── Domain/
│   ├── Entities/
│   │   ├── TeamMemberRef.swift                          (신규)
│   │   └── TeamRole.swift                                (신규)
│   └── Repositories/
│       └── TeamMemberRepository.swift                    (신규)
├── Data/
│   ├── DTOs/
│   │   └── TeamMemberRefDTO.swift                        (신규)
│   └── Repositories/
│       └── TeamMemberRepositoryImpl.swift                (신규)
└── Presentation/
    ├── ViewModels/
    │   ├── MatchDetailViewModel.swift                    (수정 — tapSummary/toast 제거)
    │   └── MatchAttendanceSummaryViewModel.swift         (신규)
    └── Views/
        ├── AttendanceVoteRow.swift                        (신규 — MATCH-3 의 voteRow 추출)
        ├── MatchDetailView.swift                          (수정 — voteRow 호출부 + 전체 보기 진입점)
        └── MatchAttendanceSummaryView.swift               (신규)
```

`Project.swift` 변경 없음 (sourcesPath glob 자동 픽업).

### 모듈 경계 결정 — 옵션 B (Match 모듈 자체)

`GET /teams/{teamId}/members` 호출이 필요한데 `DugoutTeamFeature` 가 이미 같은 endpoint 를 호출할 가능성이 있다. 다만 본 PR 에선 Feature 간 의존(Match → Team)을 추가하지 않고 Match 모듈 자체에 minimal Repository (`TeamMemberRepository`, 단 1 메서드) 를 둔다.

이유:
1. Match Feature 가 다른 Feature 에 의존하면 모듈 다이어그램(현재: Home 만 다른 Feature 의존) 위반
2. 본 PR 이 필요로 하는 멤버 필드는 minimal (userId, nickname, profileImgUrl, jerseyNumber, role, isActive) — `TeamMemberResponse` 의 부분 집합
3. `TeamRole` 은 `DugoutHomeFeature` / `DugoutTeamFeature` 가 이미 각자 독립 정의 (`Feature 독립 원칙에 따라 각자 소유` 라고 `TeamMember.swift` 주석에 명시된 패턴) — Match 도 같은 패턴 따름. 향후 공통화는 별도 리팩터링 PR. `displayName` 같은 헬퍼도 기존 정의와 동일 명세로 작성

---

## 4. 데이터 모델

### TeamMemberRef (도메인)

```swift
public struct TeamMemberRef: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64           // TeamMember.id
    public let userId: Int64       // 매칭 키
    public let nickname: String
    public let profileImgUrl: String?
    public let jerseyNumber: Int?
    public let role: TeamRole
    public let isActive: Bool
}

/// HomeFeature / TeamFeature 의 TeamRole 과 동일한 정의 — Feature 독립 원칙에 따라 각자 소유.
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

### TeamMemberRefDTO (data)

```swift
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

> 백엔드 `TeamMemberResponse` 에는 `positions: List<String>`, `joinedAt: LocalDateTime` 도 있지만 DTO 에 포함하지 않음 (Decodable 은 unknown key 무시 — Spring Boot Jackson 기본 동작과 호환).

### TeamMemberRepository

```swift
public protocol TeamMemberRepository: Sendable {
    func fetchMembers(teamId: Int64) async throws -> [TeamMemberRef]
}

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

---

## 5. ViewModel

```swift
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

        public var koreanLabel: String {
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

    /// 응답자 row (현재 filter 기준)
    public var voteRows: [AttendanceVote] {
        guard let s = loadedSnapshot else { return [] }
        let all = s.summary.votes
        switch filter {
        case .all:     return all.filter { $0.status != .maybe }   // 미정은 별도 섹션
        case .attend:  return all.filter { [.attend, .late, .earlyLeave].contains($0.status) }
        case .absent:  return all.filter { $0.status == .absent }
        case .pending: return []
        }
    }

    /// 미정 응답자 (filter == .all 에서만)
    public var maybeRows: [AttendanceVote] {
        guard filter == .all, let s = loadedSnapshot else { return [] }
        return s.summary.votes.filter { $0.status == .maybe }
    }

    /// 미응답자 row (filter == .all 또는 .pending)
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

    /// 미응답자가 0명이면 .pending 필터 제거.
    /// DGSegmentedControl 이 per-tab disable 을 지원하지 않으므로 옵션 배열에서 동적 제거.
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

### Filter 의미 표

| filter | voteRows | maybeRows | pendingRows |
|---|---|---|---|
| `.all` | attend / absent / late / earlyLeave | maybe (별도) | 미응답자 |
| `.attend` | attend + late + earlyLeave | (빈) | (빈) |
| `.absent` | absent | (빈) | (빈) |
| `.pending` | (빈) | (빈) | 미응답자 |

---

## 6. 화면 컴포지션

### MatchAttendanceSummaryView 레이아웃

```
┌─────────────────────────────────────┐
│ ← 출석 현황                          │  navigationTitle(.inline)
├─────────────────────────────────────┤
│ [DGCard] 통계                        │
│   ┌──참가──┬──불참──┬──미응답──┐    │  3컬럼 (MATCH-3 와 동일 시각)
│   │   6   │   1   │    7    │    │
│   └────────┴────────┴─────────┘    │
│   미정 1 · 늦참 0 · 조퇴 0          │
├─────────────────────────────────────┤
│ [DGSegmentedControl: Filter]         │  availableFilters 동적
│  [ 전체 ][ 참가 ][ 불참 ][ 미응답 ]  │
├─────────────────────────────────────┤
│ 응답자 N명                           │  voteRows 가 있을 때
│ ─────────────────────                │
│ AttendanceVoteRow x N                │
├─────────────────────────────────────┤
│ 미정 K명                             │  filter == .all && maybeRows 가 있을 때
│ ─────────────────────                │
│ AttendanceVoteRow x K                │
├─────────────────────────────────────┤
│ 미응답 M명                           │  pendingRows 가 있을 때
│ ─────────────────────                │
│ pendingRow x M (알림 버튼 포함)      │
└─────────────────────────────────────┘
```

### AttendanceVoteRow (공통 컴포넌트 추출)

MATCH-3 의 `MatchDetailView.voteRow(_ vote:)` private 메서드를 외부 컴포넌트로 추출:

```swift
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

`MatchDetailView` 의 `voteRow(_:)` 메서드는 삭제 + 호출부 `voteRow(vote)` → `AttendanceVoteRow(vote: vote)` 로 교체. 같은 file 의 `shortTime` static formatter 도 같이 제거.

### pendingRow (신규, MatchAttendanceSummaryView 내부)

```swift
private func pendingRow(_ member: TeamMemberRef) -> some View {
    HStack(spacing: DGSpacing.sm) {
        Text("👤")
        VStack(alignment: .leading, spacing: 2) {
            Text(member.nickname).dgText(.bodyText)
            Text(memberMetaLabel(member))
                .dgText(.label)
                .foregroundStyle(DGColor.c500)
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
```

> 알림 버튼은 native `Button` — DGButton 은 full-width 라 부적합. iOS CLAUDE.md 의 "raw Button 은 DesignSystem 내부에서만" 규칙은 이미 `AttendanceStatusButton`, `MatchListView.fab` 등 기존 Feature 내 사용 사례가 있어 본 PR 도 같은 precedent 따름. (후속 polish: `DGSmallButton` 또는 `DGPill` 컴포넌트로 추출 가능)

### filterSegment

```swift
DGSegmentedControl(
    options: viewModel.availableFilters,
    selection: $viewModel.filter
) { $0.koreanLabel }
```

미응답자 0명이면 `availableFilters == [.all, .attend, .absent]` 로 3개 노출. 현재 선택이 `.pending` 인 상태에서 미응답자가 0이 되는 케이스는 없음 (load 후 변동 없음). 안전상 ViewModel 초기값은 `.all`.

### 진입점 — MatchDetailView 의 "전체 보기" 교체

```swift
// 기존
private var summaryButton: some View {
    DGButton("전체 보기", style: .secondary) {
        viewModel.tapSummary()    // toast
    }
}

// 변경
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

`MatchDetailViewModel.tapSummary()` / `toast` 프로퍼티 / `.dgToast(item:)` modifier 제거. ViewModel 의 `toast` 가 다른 용도에 안 쓰이면 프로퍼티 자체 제거 — 단 ViewModel 의 public API 변경이므로 cross-check.

---

## 7. 네비게이션 / 에러 / PII

### 네비게이션

```
MatchListView (NavigationStack root)
   │
   │ NavigationLink(value: match.id) + navigationDestination(for: Int64.self)
   ▼
MatchDetailView
   │
   │ summaryButton: NavigationLink { MatchAttendanceSummaryView }
   ▼
MatchAttendanceSummaryView
```

`navigationDestination(for:)` 라우팅 등록은 `MatchListView` 의 기존 `Int64.self` 한 종류만. `MatchAttendanceSummaryView` push 는 destination closure 방식 — 라우팅 충돌 없음.

### 에러 처리

| 상황 | UI |
|---|---|
| 두 endpoint 중 하나 실패 (`async let` first throw) | `state = .failed(error.userMessage)`. DGErrorState 전체화면 + 재시도 |
| summary.totalMembers == 0 (이론상) | 정상 렌더 — 모든 섹션 빈 상태. fallback DGEmptyState 추가 |
| 일반 멤버가 deeplink 등으로 진입 | 진입점이 `isManager == true` 일 때만 보임. 진입 후엔 별도 가드 없음 (summary 데이터 자체는 팀원 누구나 볼 수 있는 정보) |

### PII

- `nickname`, `reason` 평문 — UI 노출 OK, 로그 금지
- `tapNotify(_:)` 콜백에서 멤버 정보 print/os_log 금지
- M6 검증 시 `grep -rn "print.*\(nickname\|reason\)" dugout-ios/Features/Match` → 0건

---

## 8. Milestone 구성

각 milestone 은 subagent 1개 dispatch. 완료 기준 = `xcodebuild -quiet build` 성공 (warning 0) + conventional commit.

| M | 범위 | 산출물 |
|---|---|---|
| **M1** | 브랜치 생성 + TeamMember Domain | `TeamMemberRef`, `TeamRole`, `TeamMemberRepository` 프로토콜 |
| **M2** | TeamMember Data | `TeamMemberRefDTO`, `TeamMemberRepositoryImpl` |
| **M3** | `AttendanceVoteRow` 공통 컴포넌트 추출 | 신규 파일 + MATCH-3 의 `voteRow` 메서드 제거 + 호출부 교체 |
| **M4** | `MatchAttendanceSummaryViewModel` + `MatchAttendanceSummaryView` (통계·필터·섹션·미응답 행·toast) | 2 파일 신규 |
| **M5** | MATCH-3 "전체 보기" 진입점 교체 | `MatchDetailView` 수정 + `MatchDetailViewModel.tapSummary`/`toast` 제거 |
| **M6** | 빌드·PII·docs/TDD.md 갱신 + 통합 커밋 + main 머지 | (controller 가 머지) |

### 수동 검증 시나리오 (M6)

1. 주장 로그인 → 경기 카드 탭 → 상세 진입
2. "전체 보기" 탭 → `MatchAttendanceSummaryView` push
3. 통계 카드 수치가 MATCH-3 와 동일
4. 필터 "전체" → 응답자(미정 제외) + 미정 별도 + 미응답 모두 노출
5. 필터 "참가" → attend/late/earlyLeave 만 노출, 미정·불참·미응답 미노출
6. 필터 "불참" → absent 만 노출
7. 필터 "미응답" → 미응답자만 노출, 각 행에 "알림" 버튼
8. "알림" 버튼 탭 → "알림 기능은 준비 중이에요" toast 3초 후 자동 사라짐
9. (코드 리뷰) 미응답자 0명 케이스에서 `availableFilters` 가 `.pending` 제거하는지 확인

### Git 전략

```bash
git checkout -b feature/phase3-match-c
# M1 ~ M6
git checkout main
git merge --no-ff feature/phase3-match-c
git branch -d feature/phase3-match-c
git push origin main
```

---

## 9. PR 완료 체크리스트

- [ ] `xcodebuild -quiet build` 성공, warnings 0
- [ ] `./gradlew compileKotlin compileTestKotlin --quiet` 성공 (baseline 점검)
- [ ] 수동 시나리오 1~8 통과
- [ ] 시나리오 9 코드 리뷰 — `availableFilters` 분기 정확
- [ ] Swift 6 Sendable 위반 0
- [ ] PII 로그 노출 0 (`grep` 0건)
- [ ] 도메인 용어 준수 (Attendance, Match, Team, TeamMember — 자유 번역·로마자 변형 없음)
- [ ] 새 ErrorCode 추가 없음
- [ ] `docs/TDD.md` 갱신 — MATCH-5 명세 추가, "출석 요약(MATCH-5)" 항목을 "후속" 에서 "완료" 로 이동
- [ ] feature/phase3-match-c 의 6 commit + main 머지 완료

---

## 10. 추후 Phase

- **Phase 3-C.1**: APNs/FCM 인프라 — 키 발급, Firebase 프로젝트, GoogleService-Info.plist, 백엔드 Firebase Admin SDK
- **Phase 3-C.2**: 디바이스 토큰 등록 + `POST /matches/{id}/notify` + 24h 쿨다운 + iOS 알림 트리거
- **Phase 3-C.3** (선택): 카카오 공유 — KakaoSDK, URL Scheme, 요약 텍스트 generate
- **MATCH-5 polish**: pull-to-refresh, 멤버 포지션 표시, stagger 애니메이션, `DGSmallButton`/`DGPill` 컴포넌트 추출
