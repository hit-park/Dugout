# Phase 4-B: 라인업 확정 + 공유 카드 Design

**작성일**: 2026-05-21
**상태**: 설계 합의 완료, implementation plan 대기
**선행**: Phase 4-A (라인업 추천·저장·수정) — main 머지 완료

---

## 1. 목표

iOS `DugoutLineupFeature` 에 (1) 라인업 확정(`POST /lineup/confirm`)과 (2) 공유 전용 카드 디자인 + 시스템 share sheet 노출 기능을 추가한다. 확정과 공유는 완전 분리된 동작이며, 공유는 백엔드/외부 SDK 의존 없이 iOS 자체 `ImageRenderer` 로 처리.

---

## 2. 스코프

### 포함
- `POST /api/v1/matches/{matchId}/lineup/confirm` 호출 → 응답으로 `isConfirmed=true` 갱신
- LineupView toolbar 우상단 "공유" 아이콘 (`square.and.arrow.up`) — `.loaded` 상태일 때 항상 노출
- LineupView managerActions 에 "확정" 버튼 추가 (`!isConfirmed` 일 때만), 기존 버튼 시각 강도 재정렬:
  - 확정 = primary
  - 편집 = secondary
  - AI 다시 추천 = tertiary
- `LineupShareCardView` 신설 — 경기 정보 + 다이아몬드 + 타순 + DH + AI 출처 푸터를 한 장(9:16 세로 1080×1920)에 담은 공유 전용 디자인
- `LineupShareSheet` — `LineupShareCardView` 를 `ImageRenderer` 로 캡쳐 후 `UIActivityViewController` 노출
- `LineupShareContext` 도메인 struct — 호출자(`MatchDetailView`)가 매치 정보를 props 로 전달
- `LineupViewModel` 에 `tapConfirm` / `tapShare` / `presentShareSheet` / `confirmingInProgress` 추가
- `MatchDetailView.lineupCard` 가 `shareContext` 전달

### 제외
- 백엔드 카드 이미지(`GET /lineup/card`) 사용 안 함 — iOS 자체 렌더
- 카카오 알림톡 / 비즈니스 채널
- 확정 후 멤버 푸시 알림 (3-C.1/2 후속)
- 공유 트래킹/통계
- 팀명(`teamName`) 자동 fetch — 본 PR 은 빈 문자열로 전달, 후속 polish에서 Team 정보 추가

### 백엔드 변경
**없음**. `POST /lineup/confirm` 은 Phase 1 단계에서 기 구현.

---

## 3. 모듈 구조

`DugoutLineupFeature` 내부에 파일 추가·수정만:

```
dugout-ios/Features/Lineup/Sources/
├── Domain/
│   ├── Entities/
│   │   └── LineupShareContext.swift            (신규)
│   └── Repositories/
│       └── LineupRepository.swift              (수정 — confirmLineup 추가)
├── Data/
│   └── Repositories/
│       └── LineupRepositoryImpl.swift          (수정 — confirmLineup 구현 추가)
└── Presentation/
    ├── ViewModels/
    │   └── LineupViewModel.swift               (수정 — tapConfirm/tapShare/presentShareSheet/confirmingInProgress/shareContext)
    └── Views/
        ├── LineupView.swift                    (수정 — toolbar 공유 아이콘 + 확정 버튼 + .sheet)
        ├── LineupShareCardView.swift           (신규)
        └── LineupShareSheet.swift              (신규 — UIActivityViewController 래퍼 포함)
```

**Match 모듈** (1 파일 수정):
- `dugout-ios/Features/Match/Sources/Presentation/Views/MatchDetailView.swift` — `lineupCard` 의 `LineupView` 호출에 `shareContext` 추가

Tuist Project.swift 변경 없음 (sourcesPath glob).

---

## 4. 데이터 모델

### LineupShareContext (Domain)

```swift
public struct LineupShareContext: Sendable, Equatable {
    public let teamName: String        // 빈 문자열 허용 (header row skip)
    public let opponentName: String?
    public let matchDate: Date
    public let matchTimeText: String   // 이미 "오후 8:00" 같은 사용자 친화 문자열
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

> 백엔드 응답 매핑이 아니라 호출자가 직접 생성하는 도메인 값. DTO 없음.

### LineupRepository 메서드 추가

```swift
public protocol LineupRepository: Sendable {
    // 기존 4개 메서드 그대로 +
    func confirmLineup(matchId: Int64) async throws -> Lineup
}
```

`LineupRepositoryImpl.confirmLineup`:
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

---

## 5. ViewModel 변경

### LineupViewModel — 추가 항목만

```swift
public let shareContext: LineupShareContext?    // init 에 추가
public var presentShareSheet: Bool = false
public var confirmingInProgress: Bool = false   // state 는 .loaded 유지, 버튼 disabled 만 토글

public init(
    matchId: Int64,
    teamId: Int64,
    isManager: Bool,
    shareContext: LineupShareContext? = nil,    // 신규
    lineupRepository: any LineupRepository = LineupRepositoryImpl(),
    attendeeRepository: any AttendeeRepository = AttendeeRepositoryImpl()
) {
    // 기존 + self.shareContext = shareContext
}

public func tapConfirm() async {
    guard case .loaded = state else { return }
    confirmingInProgress = true
    defer { confirmingInProgress = false }
    do {
        let lineup = try await lineupRepository.confirmLineup(matchId: matchId)
        state = .loaded(lineup)
        toast = DGToastItem(message: "라인업이 확정됐어요", kind: .success)
    } catch APIError.server(let r, _) where r.code == "LINEUP_ALREADY_CONFIRMED" {
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

---

## 6. 화면 컴포지션

### LineupView — toolbar 공유 아이콘

```swift
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
```

### LineupView — managerActions 재정렬 + 확정 버튼

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

> 시각 강도 = 사용자 의도 강도. 확정이 가장 강한 행동이므로 primary 승격.

### LineupView — share sheet 연결

```swift
.sheet(isPresented: $viewModel.presentShareSheet) {
    if case .loaded(let lineup) = viewModel.state {
        LineupShareSheet(
            lineup: lineup,
            shareContext: viewModel.shareContext
        )
    }
}
```

### LineupShareSheet (신규)

```swift
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

struct ShareSheetRepresentable: UIViewControllerRepresentable {
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

### LineupShareCardView (신규, 9:16 세로 1080×1920)

```
┌─────────────────────────────────────┐
│ 🟢 두갓FC                            │   teamName 있을 때만
│ 5월 28일 (토) · 오후 8:00            │
│ vs 베어스FC                          │   opponentName 있을 때만
│ 📍 잠실야구장                        │   groundName 있을 때만
├─────────────────────────────────────┤
│ (다이아몬드, 520×520)                  │   LineupDiamondView 큰 크기
├─────────────────────────────────────┤
│ 타순                                  │
│  1. 김** (SS)                       │
│  2. 박** (CF)                       │
│  ...                                  │
│  9. 이** (P)                        │
├─────────────────────────────────────┤
│ DH: 최**                             │   DH 있을 때만
├─────────────────────────────────────┤
│                  Dugout · AI 라인업  │   푸터, 우측 정렬
└─────────────────────────────────────┘
```

```swift
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

---

## 7. 진입점 (MatchDetailView 변경)

`MatchDetailView.lineupCard` 의 `LineupView` 호출에 `shareContext` 추가:

```swift
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
```

`teamName: ""` 은 의도된 빈 값 — `LineupShareCardView.header` 가 `!context.teamName.isEmpty` 가드로 row 자체 skip. 후속 polish 에서 Team fetch 시 채울 자리.

---

## 8. 에러 / 엣지 케이스

| 시나리오 | 동작 |
|---|---|
| `POST /confirm` `LINEUP_ALREADY_CONFIRMED` (race) | 자동 `load()` 재호출 + toast `"이미 확정된 라인업이에요"` |
| `POST /confirm` `TEAM_ROLE_NOT_ALLOWED` | `error.userMessage` toast (실질 발생 불가 — 버튼 isManager 가드) |
| `POST /confirm` `LINEUP_NOT_FOUND` (race) | `error.userMessage` toast |
| `POST /confirm` 네트워크 실패 | toast `"확정 중 오류가 발생했어요"` |
| `ImageRenderer.uiImage` 가 nil 반환 | `presentShareSheet` 유지, `ProgressView` 잔존 — 사용자가 dismiss. 본 PR 에선 단순화 (드문 케이스) |
| `shareContext == nil` 상태에서 공유 | 카드 header 비어있음 — 스코프상 OK (후속 polish) |
| 확정 후 "편집"/"AI 다시 추천" 시도 | `managerActions` 자체가 `!isConfirmed` 가드라 노출 안 됨. race 시 백엔드가 `LINEUP_ALREADY_CONFIRMED` 던짐 — 기존 Phase 4-A 의 inline error 분기 그대로 |

---

## 9. Milestone 구성

각 milestone subagent 1개 dispatch. 완료 기준 = `make ios-build` 성공 (warnings 0).

| M | 범위 |
|---|---|
| **M1** | 브랜치 + Repository confirm (`LineupRepository.confirmLineup` 프로토콜 + `LineupRepositoryImpl.confirmLineup` 구현) |
| **M2** | `LineupShareContext` 도메인 struct |
| **M3** | `LineupShareCardView` + `LineupShareSheet` (`UIActivityViewController` 래퍼 포함) |
| **M4** | `LineupViewModel` 수정 (tapConfirm/tapShare/presentShareSheet/confirmingInProgress/shareContext) |
| **M5** | `LineupView` 수정 (toolbar 공유 아이콘 + managerActions 확정 버튼 + .sheet 연결) |
| **M6** | `MatchDetailView.lineupCard` 의 `shareContext` props 전달 + 빌드/PII/TDD.md/통합 커밋 + main 머지 |

### 수동 검증 시나리오 (M6)

1. 주장 로그인 → 경기 상세 → 라인업 → AI 추천 → 저장 → loaded 상태
2. 우상단 공유 아이콘 탭 → ProgressView → UIActivityViewController 노출
3. 카드 미리보기: 경기 정보(날짜·상대·구장) + 다이아몬드 + 타순 + DH + 푸터
4. "사진 저장" 선택 → 사진 앱 저장 확인
5. share sheet 닫고 LineupView 복귀
6. "확정" 탭 → 로딩 → toast "라인업이 확정됐어요" → managerActions 사라짐
7. 공유 아이콘 여전히 노출 → 다시 탭 → 동일 카드 공유 가능
8. (코드 리뷰) 확정 race 시 `LINEUP_ALREADY_CONFIRMED` 분기 + 자동 reload

### Git 전략

```bash
git checkout -b feature/phase4-lineup-b
# M1 ~ M6
git checkout main && git merge --no-ff feature/phase4-lineup-b
git branch -d feature/phase4-lineup-b && git push origin main
```

---

## 10. PR 완료 체크리스트

- [ ] `make ios-build` 성공 (warnings 0)
- [ ] `make api-test` 성공 (baseline)
- [ ] 수동 시나리오 1~7 통과, 8은 코드 리뷰
- [ ] Swift 6 Sendable 위반 0
- [ ] PII 로그 노출 0
- [ ] 새 ErrorCode 추가 없음
- [ ] `docs/TDD.md` DugoutLineupFeature 단락 갱신 (확정·공유 추가)
- [ ] 6 commit + main 머지 + push

---

## 11. 추후 Phase

- **팀명 자동 fetch**: `LineupShareContext.teamName` 을 호출자가 채우려면 Team 정보 필요. `MatchDetailViewModel` 이 Match + Team 조인 fetch 또는 별도 캐시.
- **카드 디자인 polish**: 다이아몬드에 jerseyNumber 표시, 팀 로고, 배경 그라데이션, QR 코드(앱 다운로드 링크) 등
- **백엔드 카드 이미지**: `GET /lineup/card` 가 실제 S3 PNG/PDF 응답하도록 구현
- **카카오 알림톡**: 비즈니스 채널 등록 + AlimtalkClient + 멤버 전체 발송
- **공유 트래킹**: 어떤 채널로 공유했는지 통계
