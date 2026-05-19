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
                        MatchCalendarGrid(
                            displayedMonth: viewModel.displayedMonth,
                            selectedDate: viewModel.selectedDate,
                            hasMatch: viewModel.hasMatch(on:),
                            onSelect: { date in viewModel.selectedDate = date }
                        )
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
            Button {
                Task { await viewModel.goPreviousMonth() }
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(DGColor.c700)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            Spacer()
            Text(viewModel.monthLabel).dgText(.sectionTitle)
            Spacer()
            Button {
                Task { await viewModel.goNextMonth() }
            } label: {
                Image(systemName: "chevron.right")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(DGColor.c700)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
        }
    }

    @ViewBuilder
    private var matchList: some View {
        switch viewModel.state {
        case .idle, .loading:
            DGLoadingState(preset: .list)
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
                    DGBadge(match.dDayLabel, variant: .dDay)
                    Text(matchDateLabel(match)).dgText(.subText).foregroundStyle(DGColor.c500)
                    Spacer()
                    DGBadge(match.status.koreanLabel, variant: .neutral)
                }
                Text("vs \(match.opponentName ?? "상대 미정")").dgText(.cardTitle)
                if let ground = match.groundName {
                    Text("📍 \(ground)").dgText(.subText).foregroundStyle(DGColor.c500)
                }
            }
        }
    }

    private static let matchDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 (E)"
        return f
    }()

    private func matchDateLabel(_ match: Match) -> String {
        "\(Self.matchDateFormatter.string(from: match.matchDate)) · \(match.matchTime.displayString)"
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
