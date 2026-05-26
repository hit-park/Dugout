//
//  LineupView.swift
//  DugoutLineupFeature

import SwiftUI
import DugoutDesignSystem

public struct LineupView: View {
    @State private var viewModel: LineupViewModel

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

    public var body: some View {
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
                    jerseyNumber: nil
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
}
