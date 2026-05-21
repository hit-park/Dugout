# Phase 4-B Implementation Plan: 라인업 확정 + 공유 카드

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** iOS `DugoutLineupFeature` 에 라인업 확정(`POST /lineup/confirm`)과 공유 전용 카드 + 시스템 share sheet 를 추가한다. 확정과 공유는 완전 분리. 공유는 백엔드 변경 없이 iOS `ImageRenderer` 자체 렌더.

**Architecture:** `LineupRepository` 에 `confirmLineup` 메서드 추가. `LineupShareContext` 도메인 struct 신설. `LineupShareCardView` (9:16 세로 1080×1920) + `LineupShareSheet` (UIActivityViewController 래퍼) 신설. `LineupViewModel` 에 `tapConfirm`/`tapShare`/`presentShareSheet`/`confirmingInProgress`/`shareContext` 추가. `LineupView` toolbar 에 공유 아이콘 + managerActions 에 "확정" primary 버튼. `MatchDetailView.lineupCard` 가 `shareContext` props 전달.

**Tech Stack:** Swift 6 (Strict Concurrency), SwiftUI, `ImageRenderer` (iOS 16+), `UIActivityViewController`. 백엔드 변경 없음.

---

## 0. 사전 준비 (PLAN ONLY, NOT A TASK)

### 빌드 / 검증 명령

```bash
# 매 task 끝
cd /Users/heetae/Documents/Source/Dugout
make ios-build       # warnings 0

# Link cache 에러 시
make clean && make ios-build

# 마지막 통합 검증
make seed-check      # 4개 서비스 UP
```

### 베이스 브랜치

```bash
git checkout main
git pull origin main
git checkout -b feature/phase4-lineup-b
```

### 백엔드 endpoint (이미 구현)

`POST /api/v1/matches/{matchId}/lineup/confirm` (no body) → `200 LineupResponse` (isConfirmed=true, confirmedAt 채워짐). 권한: 주장/매니저. 에러: `LINEUP_NOT_FOUND`, `LINEUP_ALREADY_CONFIRMED` (idempotency 아님 — 이미 confirmed 면 에러 던짐), `TEAM_ROLE_NOT_ALLOWED`.

### 재사용 / 신규 / 수정 파일

**신규**:
- `dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupShareContext.swift`
- `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupShareCardView.swift`
- `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupShareSheet.swift`

**수정**:
- `dugout-ios/Features/Lineup/Sources/Domain/Repositories/LineupRepository.swift` (`confirmLineup` 메서드 추가)
- `dugout-ios/Features/Lineup/Sources/Data/Repositories/LineupRepositoryImpl.swift` (`confirmLineup` 구현)
- `dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupViewModel.swift` (`shareContext` / `tapConfirm` / `tapShare` / `presentShareSheet` / `confirmingInProgress` 추가)
- `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift` (toolbar 공유 아이콘 + managerActions 확정 버튼 + .sheet)
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift` (`lineupCard` 의 `LineupView` 호출에 `shareContext` 추가)
- `docs/TDD.md` (DugoutLineupFeature 단락 갱신)

---

## Milestone 1 — 브랜치 + Repository confirm

### Task 1.1: 브랜치

```bash
cd /Users/heetae/Documents/Source/Dugout
git status   # clean 확인
git checkout main
git pull origin main
git checkout -b feature/phase4-lineup-b
```

### Task 1.2: LineupRepository 프로토콜에 confirmLineup 추가

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Repositories/LineupRepository.swift`

Read the file first, then add `confirmLineup` method to the protocol. Final form:

```swift
public protocol LineupRepository: Sendable {
    func fetchLineup(matchId: Int64) async throws -> Lineup
    func recommend(matchId: Int64, attendees: [Attendee]) async throws -> LineupRecommendation
    func saveLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
    func updateLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
    func confirmLineup(matchId: Int64) async throws -> Lineup
}
```

기존 doc comment 유지. `SaveLineupRequest` struct 도 그대로.

### Task 1.3: LineupRepositoryImpl 에 confirmLineup 구현 추가

**File**: `dugout-ios/Features/Lineup/Sources/Data/Repositories/LineupRepositoryImpl.swift`

`updateLineup` 메서드 다음에 추가:

```swift
public func confirmLineup(matchId: Int64) async throws -> Lineup {
    let endpoint = APIEndpoint(
        path: "/api/v1/matches/\(matchId)/lineup/confirm",
        method: .post
    )
    let dto: LineupDTO = try await client.request(endpoint)
    guard let lineup = dto.toDomain() else {
        throw APIError.decoding("LineupDTO → Lineup 변환 실패")
    }
    return lineup
}
```

### Task 1.4: 빌드 + 커밋

```bash
make ios-build       # warnings 0
```

```bash
git add dugout-ios/Features/Lineup/Sources/Domain/Repositories/LineupRepository.swift \
        dugout-ios/Features/Lineup/Sources/Data/Repositories/LineupRepositoryImpl.swift
git commit -m "feat(ios): LineupRepository.confirmLineup 추가 (Phase 4-B 1/6)"
```

---

## Milestone 2 — LineupShareContext 도메인 struct

### Task 2.1: LineupShareContext 파일 작성

**File**: `dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupShareContext.swift`

```swift
//
//  LineupShareContext.swift
//  DugoutLineupFeature
//
//  공유 카드에 표시할 매치 컨텍스트. 호출자(MatchDetailView)가 직접 생성하여
//  LineupView 의 init 으로 전달. 백엔드 응답 매핑이 아니며 DTO 없음.
//

import Foundation

public struct LineupShareContext: Sendable, Equatable {
    /// 팀 이름. 빈 문자열이면 카드 헤더의 팀 row 자체를 skip.
    public let teamName: String

    /// 상대팀 이름. nil 이면 "vs ..." row skip.
    public let opponentName: String?

    /// 경기 날짜.
    public let matchDate: Date

    /// 경기 시간 표시 문자열 (예: "오후 8:00"). TimeOfDay.displayString 결과를 그대로 받음.
    public let matchTimeText: String

    /// 구장 이름. nil 이면 "📍 ..." row skip.
    public let groundName: String?

    public init(
        teamName: String,
        opponentName: String?,
        matchDate: Date,
        matchTimeText: String,
        groundName: String?
    ) {
        self.teamName = teamName
        self.opponentName = opponentName
        self.matchDate = matchDate
        self.matchTimeText = matchTimeText
        self.groundName = groundName
    }
}
```

### Task 2.2: 빌드 + 커밋

```bash
make ios-build
```

```bash
git add dugout-ios/Features/Lineup/Sources/Domain/Entities/LineupShareContext.swift
git commit -m "feat(ios): LineupShareContext 도메인 struct (Phase 4-B 2/6)"
```

---

## Milestone 3 — LineupShareCardView + LineupShareSheet

### Task 3.1: LineupShareCardView

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupShareCardView.swift`

```swift
//
//  LineupShareCardView.swift
//  DugoutLineupFeature
//
//  공유 전용 9:16 세로 카드. 1080×1920 으로 ImageRenderer 캡쳐.
//

import SwiftUI
import DugoutDesignSystem

struct LineupShareCardView: View {
    let lineup: Lineup
    let context: LineupShareContext?

    var body: some View {
        VStack(alignment: .leading, spacing: DGSpacing.xl) {
            header
            diamondSection
            battingOrderSection
            if let dh = lineup.entries.first(where: { $0.position == .designatedHitter && !$0.isBench }) {
                dhSection(dh)
            }
            Spacer(minLength: 0)
            footer
        }
        .padding(.horizontal, 48)
        .padding(.vertical, 64)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.c0)
    }

    @ViewBuilder
    private var header: some View {
        if let context {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                if !context.teamName.isEmpty {
                    HStack(spacing: DGSpacing.sm) {
                        Text("🟢")
                        Text(context.teamName)
                            .font(DGFont.pretendard(.bold, size: 36))
                            .foregroundStyle(DGColor.c900)
                    }
                }
                Text(headerDateLine(context))
                    .font(DGFont.pretendard(.semibold, size: 22))
                    .foregroundStyle(DGColor.c700)
                if let opponent = context.opponentName {
                    Text("vs \(opponent)")
                        .font(DGFont.pretendard(.regular, size: 22))
                        .foregroundStyle(DGColor.c700)
                }
                if let ground = context.groundName {
                    Text("📍 \(ground)")
                        .font(DGFont.pretendard(.regular, size: 20))
                        .foregroundStyle(DGColor.c500)
                }
            }
        }
    }

    private var diamondSection: some View {
        let fieldEntries = lineup.entries.filter { !$0.isBench && $0.position.isField }
        let dict = Dictionary(uniqueKeysWithValues: fieldEntries.map { entry in
            (entry.position, LineupDiamondView.PositionOccupant(
                nickname: entry.nickname, jerseyNumber: nil
            ))
        })
        return LineupDiamondView(entriesByPosition: dict)
            .frame(width: 520, height: 520)
            .frame(maxWidth: .infinity)
    }

    private var battingOrderSection: some View {
        let ordered = lineup.entries
            .filter { !$0.isBench && $0.battingOrder != nil }
            .sorted { ($0.battingOrder ?? 0) < ($1.battingOrder ?? 0) }
        return VStack(alignment: .leading, spacing: DGSpacing.sm) {
            Text("타순")
                .font(DGFont.pretendard(.bold, size: 22))
                .foregroundStyle(DGColor.c900)
            ForEach(ordered) { entry in
                HStack(spacing: DGSpacing.md) {
                    Text("\(entry.battingOrder ?? 0)")
                        .font(DGFont.pretendard(.bold, size: 24))
                        .foregroundStyle(DGColor.p500)
                        .frame(width: 32, alignment: .leading)
                    Text(entry.nickname)
                        .font(DGFont.pretendard(.regular, size: 22))
                    Text("(\(entry.position.shortName))")
                        .font(DGFont.pretendard(.regular, size: 20))
                        .foregroundStyle(DGColor.c500)
                }
            }
        }
    }

    private func dhSection(_ entry: LineupEntry) -> some View {
        HStack(spacing: DGSpacing.md) {
            Text("DH")
                .font(DGFont.pretendard(.bold, size: 22))
                .foregroundStyle(DGColor.p500)
            Text(entry.nickname).font(DGFont.pretendard(.regular, size: 22))
        }
    }

    private var footer: some View {
        HStack {
            Spacer()
            Text("Dugout · AI 라인업")
                .font(DGFont.pretendard(.semibold, size: 16))
                .foregroundStyle(DGColor.c500)
        }
    }

    private func headerDateLine(_ ctx: LineupShareContext) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 (E)"
        return "\(f.string(from: ctx.matchDate)) · \(ctx.matchTimeText)"
    }
}
```

### Task 3.2: LineupShareSheet

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupShareSheet.swift`

```swift
//
//  LineupShareSheet.swift
//  DugoutLineupFeature
//
//  ImageRenderer 로 LineupShareCardView 를 UIImage 로 캡쳐한 뒤
//  UIActivityViewController(시스템 share sheet)에 전달.
//

import SwiftUI
import UIKit

struct LineupShareSheet: View {
    let lineup: Lineup
    let shareContext: LineupShareContext?
    @Environment(\.dismiss) private var dismiss
    @State private var renderedImage: UIImage?

    var body: some View {
        Group {
            if let image = renderedImage {
                ShareSheetRepresentable(items: [image]) { dismiss() }
            } else {
                ProgressView()
                    .task { renderedImage = await renderCard() }
            }
        }
    }

    @MainActor
    private func renderCard() async -> UIImage? {
        let card = LineupShareCardView(lineup: lineup, context: shareContext)
            .frame(width: 1080, height: 1920)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 2.0
        return renderer.uiImage
    }
}

private struct ShareSheetRepresentable: UIViewControllerRepresentable {
    let items: [Any]
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        vc.completionWithItemsHandler = { _, _, _, _ in onDismiss() }
        return vc
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
```

### Task 3.3: 빌드 + 커밋

```bash
make ios-build
```

> SwiftUI View 가 어디서도 호출되지 않아도 컴파일 가능 (dead-code 경고 없음).

```bash
git add dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupShareCardView.swift \
        dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupShareSheet.swift
git commit -m "feat(ios): LineupShareCardView + LineupShareSheet (Phase 4-B 3/6)"
```

---

## Milestone 4 — LineupViewModel 수정

### Task 4.1: LineupViewModel 에 shareContext / 액션 / 플래그 추가

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupViewModel.swift`

Read the file first to confirm current init/state structure.

- [ ] **Step A**: 프로퍼티 추가

기존 `public var toast: DGToastItem? = nil` 줄 뒤에 추가:

```swift
    public var presentShareSheet: Bool = false
    public var confirmingInProgress: Bool = false
    public let shareContext: LineupShareContext?
```

(`shareContext` 는 `public let`, init 에서만 할당)

- [ ] **Step B**: init 시그니처 + body 갱신

기존:

```swift
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
```

다음으로 교체:

```swift
    public init(
        matchId: Int64,
        teamId: Int64,
        isManager: Bool,
        shareContext: LineupShareContext? = nil,
        lineupRepository: any LineupRepository = LineupRepositoryImpl(),
        attendeeRepository: any AttendeeRepository = AttendeeRepositoryImpl()
    ) {
        self.matchId = matchId
        self.teamId = teamId
        self.isManager = isManager
        self.shareContext = shareContext
        self.lineupRepository = lineupRepository
        self.attendeeRepository = attendeeRepository
    }
```

- [ ] **Step C**: tapConfirm / tapShare 메서드 추가

기존 `onEditCompleted` / `onEditCancelled` 메서드 다음에 추가:

```swift
    public func tapConfirm() async {
        guard case .loaded = state else { return }
        confirmingInProgress = true
        defer { confirmingInProgress = false }
        do {
            let lineup = try await lineupRepository.confirmLineup(matchId: matchId)
            state = .loaded(lineup)
            toast = DGToastItem(message: "라인업이 확정됐어요", kind: .success)
        } catch APIError.server(let response, _) where response.code == "LINEUP_ALREADY_CONFIRMED" {
            await load()
            toast = DGToastItem(message: "이미 확정된 라인업이에요", kind: .info)
        } catch let error as APIError {
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            toast = DGToastItem(message: "확정 중 오류가 발생했어요", kind: .danger)
        }
    }

    public func tapShare() {
        guard case .loaded = state else { return }
        presentShareSheet = true
    }
```

### Task 4.2: 빌드 + 커밋

```bash
make ios-build
```

```bash
git add dugout-ios/Features/Lineup/Sources/Presentation/ViewModels/LineupViewModel.swift
git commit -m "feat(ios): LineupViewModel — tapConfirm/tapShare + shareContext (Phase 4-B 4/6)"
```

---

## Milestone 5 — LineupView 수정

### Task 5.1: LineupView init 에 shareContext 받아 ViewModel 로 전달

**File**: `dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift`

기존 init:

```swift
    public init(matchId: Int64, teamId: Int64, isManager: Bool) {
        _viewModel = State(
            initialValue: LineupViewModel(
                matchId: matchId, teamId: teamId, isManager: isManager
            )
        )
    }
```

다음으로 교체:

```swift
    public init(
        matchId: Int64,
        teamId: Int64,
        isManager: Bool,
        shareContext: LineupShareContext? = nil
    ) {
        _viewModel = State(
            initialValue: LineupViewModel(
                matchId: matchId,
                teamId: teamId,
                isManager: isManager,
                shareContext: shareContext
            )
        )
    }
```

### Task 5.2: toolbar 공유 아이콘 + .sheet 연결

`LineupView.body` 의 modifier 체인에 추가. 기존:

```swift
        content
            .background(DGColor.c100)
            .navigationTitle("라인업")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.load() }
            .dgToast(item: $viewModel.toast)
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

다음으로 교체 (`.toolbar` 추가 + share `.sheet` 추가):

```swift
        content
            .background(DGColor.c100)
            .navigationTitle("라인업")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if case .loaded = viewModel.state {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            viewModel.tapShare()
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                        }
                    }
                }
            }
            .task { await viewModel.load() }
            .dgToast(item: $viewModel.toast)
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
            .sheet(isPresented: $viewModel.presentShareSheet) {
                if case .loaded(let lineup) = viewModel.state {
                    LineupShareSheet(
                        lineup: lineup,
                        shareContext: viewModel.shareContext
                    )
                }
            }
```

### Task 5.3: managerActions 재정렬 + 확정 버튼 추가

기존 `managerActions`:

```swift
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
```

다음으로 교체:

```swift
    private var managerActions: some View {
        VStack(spacing: DGSpacing.md) {
            DGButton(
                "확정",
                style: .primary,
                isLoading: viewModel.confirmingInProgress,
                isEnabled: !viewModel.confirmingInProgress
            ) {
                Task { await viewModel.tapConfirm() }
            }
            DGButton("편집", style: .secondary) {
                Task { await viewModel.tapEditExisting() }
            }
            DGButton("AI 다시 추천", style: .tertiary) {
                Task { await viewModel.tapRecommend() }
            }
        }
    }
```

### Task 5.4: 빌드 + 커밋

```bash
make ios-build
```

Link cache 에러 시 `make clean && make ios-build`.

```bash
git add dugout-ios/Features/Lineup/Sources/Presentation/Views/LineupView.swift
git commit -m "feat(ios): LineupView 공유 아이콘 + 확정 버튼 (Phase 4-B 5/6)"
```

---

## Milestone 6 — 통합 (MatchDetailView + 문서 + 머지)

### Task 6.1: MatchDetailView.lineupCard 에 shareContext 추가

**File**: `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift`

기존 `lineupCard`:

```swift
private func lineupCard(_ match: Match) -> some View {
    NavigationLink {
        LineupView(
            matchId: match.id,
            teamId: match.teamId,
            isManager: viewModel.isManager
        )
    } label: {
        ...
    }
    .buttonStyle(.plain)
}
```

`LineupView` 호출에 `shareContext` 인자 추가:

```swift
private func lineupCard(_ match: Match) -> some View {
    NavigationLink {
        LineupView(
            matchId: match.id,
            teamId: match.teamId,
            isManager: viewModel.isManager,
            shareContext: LineupShareContext(
                teamName: "",
                opponentName: match.opponentName,
                matchDate: match.matchDate,
                matchTimeText: match.matchTime.displayString,
                groundName: match.groundName
            )
        )
    } label: {
        ...
    }
    .buttonStyle(.plain)
}
```

> `teamName: ""` 의도된 빈 값. 후속 polish 에서 Team fetch.

### Task 6.2: 빌드 + PII 점검

```bash
make ios-build
```

```bash
grep -rEn "(print|os_log|NSLog).*(nickname|jersey|attendee|member)" \
  dugout-ios/Features/Lineup/Sources/
```

Expected: 0 lines.

### Task 6.3: docs/TDD.md DugoutLineupFeature 단락 갱신

**File**: `docs/TDD.md`

기존 (Phase 4-A 후):

> `DugoutLineupFeature`는 Phase 4-A 에서 추가된 모듈로, 백엔드 `/api/v1/matches/{matchId}/lineup`(GET/POST/PUT) 과 `/recommend`, 그리고 출석자 조인용으로 `/api/v1/matches/{matchId}/attendance` + `/api/v1/teams/{teamId}/members` 엔드포인트를 사용한다. 주장/매니저는 AI 추천(헝가리안 알고리즘 — dugout-ai)을 받아 편집·저장하고, 일반 멤버는 결과를 readonly 로 조회한다. 라인업 확정(`/confirm`)·카드 이미지 공유(`/card`)는 Phase 4-B 예정이다.

다음으로 교체:

> `DugoutLineupFeature`는 Phase 4-A·B 에서 추가된 모듈로, 백엔드 `/api/v1/matches/{matchId}/lineup`(GET/POST/PUT) + `/recommend` + `/confirm` 엔드포인트를 사용한다. 출석자 조인용으로 `/api/v1/matches/{matchId}/attendance` + `/api/v1/teams/{teamId}/members` 도 호출. 주장/매니저는 AI 추천(헝가리안 알고리즘 — dugout-ai)을 받아 편집·저장·확정하고, 라인업 공유는 iOS 자체 렌더(`ImageRenderer` + `UIActivityViewController`)로 처리한다 (백엔드 `/card` 미사용). 일반 멤버는 결과를 readonly 로 조회. 카카오 알림톡·푸시 알림은 후속 Phase 예정이다.

### Task 6.4: 통합 커밋

```bash
cd /Users/heetae/Documents/Source/Dugout
git status      # MatchDetailView + TDD.md 만 변경 확인
git diff --stat
```

If only these 2 files modified:

```bash
git add dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift docs/TDD.md
git commit -m "feat(ios): MATCH-3 lineupCard shareContext 전달 + docs(tdd) Phase 4-B 반영 (Phase 4-B 6/6)"
```

Unexpected file modified → BLOCKED 보고.

### Task 6.5: 시뮬레이터 수동 시나리오 (CONTROLLER)

1. 주장 로그인 → 경기 상세 → 라인업 → AI 추천 → 저장 → loaded 상태
2. 우상단 공유 아이콘 탭 → ProgressView → UIActivityViewController 노출
3. 카드 미리보기: 경기 정보 + 다이아몬드 + 타순 + DH + 푸터
4. "사진 저장" → 사진 앱 저장 확인
5. share sheet 닫고 LineupView 복귀
6. "확정" → 로딩 → toast → managerActions 사라짐
7. 공유 아이콘 다시 → 동일 카드
8. (코드 리뷰) `LINEUP_ALREADY_CONFIRMED` race 분기 + 자동 reload

### Task 6.6: main 머지 (CONTROLLER)

```bash
git checkout main
git merge --no-ff feature/phase4-lineup-b -m "Merge branch 'feature/phase4-lineup-b'

Phase 4-B 완료 (라인업 확정 + 공유 카드):
- LineupRepository.confirmLineup (POST /lineup/confirm)
- LineupShareContext 도메인 struct (호출자 props 전달)
- LineupShareCardView 9:16 세로 카드 (1080×1920)
- LineupShareSheet — ImageRenderer + UIActivityViewController
- LineupView toolbar 공유 아이콘 + managerActions 확정 primary 버튼
- MatchDetailView.lineupCard shareContext 전달
- 백엔드 변경 0"

git branch -d feature/phase4-lineup-b
git push origin main
```

---

## 검증 체크리스트

- [ ] `make ios-build` 성공 (warnings 0)
- [ ] `make api-test` 성공 (baseline)
- [ ] 수동 시나리오 1~7 통과, 8은 코드 리뷰
- [ ] Swift 6 Sendable 위반 0
- [ ] PII 로그 노출 0
- [ ] 새 ErrorCode 추가 없음
- [ ] `docs/TDD.md` DugoutLineupFeature 단락 갱신
- [ ] 6 commit + main 머지 + push 완료

---

## 추후 Phase

- 팀명 자동 fetch (`LineupShareContext.teamName` 채우기)
- 카드 디자인 polish (jersey 표시, 팀 로고, 배경, QR)
- 백엔드 `GET /lineup/card` 실제 S3 PNG 구현
- 카카오 알림톡 비즈니스 채널
- 공유 트래킹 통계
