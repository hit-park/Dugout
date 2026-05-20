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

    // MARK: - 1) 통계 카드

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

    // MARK: - 2) 필터

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
