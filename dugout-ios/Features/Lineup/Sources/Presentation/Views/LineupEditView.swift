//
//  LineupEditView.swift
//  DugoutLineupFeature

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
