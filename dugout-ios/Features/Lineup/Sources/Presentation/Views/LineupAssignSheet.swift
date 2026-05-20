//
//  LineupAssignSheet.swift
//  DugoutLineupFeature

import SwiftUI
import DugoutDesignSystem

struct LineupAssignSheet: View {
    @Environment(\.dismiss) private var dismiss

    let target: LineupDraftEntry
    let draft: LineupDraft
    let onConfirm: (BaseballPosition, Int?, Bool) -> Void

    @State private var selectedPosition: BaseballPosition?
    @State private var selectedBattingOrder: Int?
    @State private var goBench: Bool = false

    init(
        target: LineupDraftEntry,
        draft: LineupDraft,
        onConfirm: @escaping (BaseballPosition, Int?, Bool) -> Void
    ) {
        self.target = target
        self.draft = draft
        self.onConfirm = onConfirm
        if target.isBench {
            _selectedPosition = State(initialValue: nil)
            _selectedBattingOrder = State(initialValue: nil)
            _goBench = State(initialValue: false)
        } else {
            _selectedPosition = State(initialValue: target.position)
            _selectedBattingOrder = State(initialValue: target.battingOrder)
            _goBench = State(initialValue: false)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    contextCard
                    positionSection
                    battingOrderSection
                    confirmButton
                }
                .padding(DGSpacing.lg)
            }
            .background(DGColor.c100)
            .navigationTitle("\(target.nickname) 배정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
            }
        }
    }

    private var contextCard: some View {
        DGCard {
            HStack {
                Text("👤")
                Text(target.nickname).dgText(.bodyText)
                if let jersey = target.jerseyNumber {
                    Text("#\(jersey)").dgText(.subText).foregroundStyle(DGColor.c500)
                }
                Spacer()
            }
        }
    }

    private var positionSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("포지션").dgText(.cardTitle)
                let columns = Array(repeating: GridItem(.flexible(), spacing: DGSpacing.sm), count: 4)
                LazyVGrid(columns: columns, spacing: DGSpacing.sm) {
                    ForEach(BaseballPosition.allCases, id: \.self) { pos in
                        positionChip(pos)
                    }
                    benchChip
                }
            }
        }
    }

    private func positionChip(_ pos: BaseballPosition) -> some View {
        let occupant = draft.entries.first(where: {
            !$0.isBench && $0.position == pos && $0.userId != target.userId
        })
        let isOccupied = occupant != nil
        let isSelected = !goBench && selectedPosition == pos
        return Button {
            goBench = false
            selectedPosition = pos
        } label: {
            VStack(spacing: 2) {
                Text(pos.shortName)
                    .font(DGFont.pretendard(.semibold, size: 13))
                    .foregroundStyle(isSelected ? .white : DGColor.c900)
                if isOccupied, let nick = occupant?.nickname {
                    Text("\(nick.prefix(2))님")
                        .font(DGFont.pretendard(.regular, size: 10))
                        .foregroundStyle(isSelected ? .white.opacity(0.8) : DGColor.c500)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(isSelected ? DGColor.p500 : DGColor.c0)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.button)
                    .stroke(isSelected ? DGColor.p500 : DGColor.c200, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var benchChip: some View {
        Button {
            goBench = true
            selectedPosition = nil
            selectedBattingOrder = nil
        } label: {
            Text("벤치")
                .font(DGFont.pretendard(.semibold, size: 13))
                .foregroundStyle(goBench ? .white : DGColor.c900)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(goBench ? DGColor.c700 : DGColor.c0)
                .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.button)
                        .stroke(goBench ? DGColor.c700 : DGColor.c200, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var battingOrderSection: some View {
        if !goBench {
            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.md) {
                    Text("타순").dgText(.cardTitle)
                    if selectedPosition == nil {
                        Text("포지션을 먼저 선택해주세요")
                            .dgText(.label)
                            .foregroundStyle(DGColor.c500)
                    }
                    let columns = Array(repeating: GridItem(.flexible(), spacing: DGSpacing.sm), count: 5)
                    LazyVGrid(columns: columns, spacing: DGSpacing.sm) {
                        ForEach(1...9, id: \.self) { order in
                            battingOrderChip(order)
                        }
                    }
                }
            }
        }
    }

    private func battingOrderChip(_ order: Int) -> some View {
        let occupant = draft.entries.first(where: {
            $0.battingOrder == order && $0.userId != target.userId && !$0.isBench
        })
        let isOccupied = occupant != nil
        let isSelected = selectedBattingOrder == order
        let isEnabled = selectedPosition != nil
        return Button {
            selectedBattingOrder = order
        } label: {
            Text("\(order)")
                .font(DGFont.pretendard(.semibold, size: 14))
                .foregroundStyle(isSelected ? .white : (isEnabled ? DGColor.c900 : DGColor.c300))
                .frame(maxWidth: .infinity, minHeight: 40)
                .background(isSelected ? DGColor.p500 : DGColor.c0)
                .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.button)
                        .stroke(
                            isSelected ? DGColor.p500
                                : (isOccupied ? DGColor.c300 : DGColor.c200),
                            lineWidth: 1
                        )
                )
                .opacity(isEnabled ? 1.0 : 0.5)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
    }

    private var confirmButton: some View {
        DGButton(
            "확정",
            style: .primary,
            isEnabled: canConfirm
        ) {
            if goBench {
                onConfirm(.designatedHitter, nil, true)
            } else if let pos = selectedPosition, let order = selectedBattingOrder {
                onConfirm(pos, order, false)
            }
            dismiss()
        }
    }

    private var canConfirm: Bool {
        if goBench { return true }
        return selectedPosition != nil && selectedBattingOrder != nil
    }
}
