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
                            AttendanceVoteRow(vote: vote)
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


    // MARK: - 4) 주장 전용 전체 보기 버튼

    private var summaryButton: some View {
        DGButton("전체 보기", style: .secondary) {
            viewModel.tapSummary()
        }
    }

    // MARK: - Date Formatters

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

    private static func matchDateLabel(_ match: Match) -> String {
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

}
